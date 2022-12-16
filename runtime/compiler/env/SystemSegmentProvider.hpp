/*******************************************************************************
 * Copyright IBM Corp. and others 2000
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 *******************************************************************************/

#ifndef J9_SYSTEMSEGMENTPROVIDER_H
#define J9_SYSTEMSEGMENTPROVIDER_H

#include <set>
#include <deque>
#include <stdio.h>
#include "env/SegmentAllocator.hpp"
#include "env/MemorySegment.hpp"

#include "env/J9SegmentAllocator.hpp"
#include "env/TypedAllocator.hpp"
#include "infra/ReferenceWrapper.hpp"
#include "env/RawAllocator.hpp"

class RegionLog
   {
   public:
   RegionLog *_next;
   void printRegionLog(FILE *file);
   };  // forward declaration of reginLog for list of regionlogs maintained in a segment provider

// printRegionLogList of regionLogs after one head pointer. 
// In the end, all regions in a compilation will be collected in a double linked list of regionLogs and the head and tail maintained in segment provider.
// On destruction of a segment provider, dump results/insert the list to global list
void printRegionLogList(RegionLog *head, FILE *file)
   {
   while (head)
      {
      head->printRegionLog(file);
      head = head->_next;
      }
   }

// print all inserted compilations
void printAllCompilations(FILE *file)
   {
   if (!_globalCompilationsList) return;
   for (auto &compilation : *_globalCompilationsList)
      {
      fprintf(file, "Compilation %d, Memory usage: %zu\n", std::get<0>(compilation), std::get<1>(compilation));
      printRegionLogList(std::get<2>(compilation), file);
      }
   }

namespace J9 {

class SystemSegmentProvider : public TR::SegmentAllocator
   {
public:
   SystemSegmentProvider(size_t defaultSegmentSize, size_t systemSegmentSize, size_t allocationLimit, J9::J9SegmentProvider &segmentAllocator, TR::RawAllocator rawAllocator);
   ~SystemSegmentProvider() throw();
   virtual TR::MemorySegment &request(size_t requiredSize);
   virtual void release(TR::MemorySegment &segment) throw();
   size_t systemBytesAllocated() const throw();
   size_t regionBytesAllocated() const throw();
   size_t bytesAllocated() const throw();
   // new counters added
   size_t regionBytesInUse() const throw();
   size_t regionRealBytesInUse() const throw();
   size_t allocationLimit() const throw();
   void setAllocationLimit(size_t allocationLimit);
   bool isLargeSegment(size_t segmentSize);
   // set to collect in a segment provider
   void setCollectRegionLog() { _collectRegionLog = true; }

   uint32_t recordEvent() { return ++_timestamp; }    // called on creation and destructor of region
   bool collectRegions() { return _recordRegions; }   // called in constructor of region to check if region should be allocated
   // head and tail for the double linked list for regionlogs.
   RegionLog *_regionLogListHead = NULL;
   RegionLog *_regionLogListTail = NULL;

   static std::vector<std::tuple<uint32_t, size_t, RegionLog *>> *_globalCompilationsList = NULL;  // list of all compilations, <compilation number, bytesAllocated, list of regionLog>

private:
   size_t round(size_t requestedSize);
   ptrdiff_t remaining(const J9MemorySegment &memorySegment);
   TR::MemorySegment &allocateNewSegment(size_t size, TR::reference_wrapper<J9MemorySegment> systemSegment);
   TR::MemorySegment &createSegmentFromArea(size_t size, void * segmentArea);

   // _systemSegmentSize is only to be written once in the constructor
   size_t _systemSegmentSize;
   size_t _allocationLimit;
   size_t _systemBytesAllocated;
   size_t _regionBytesAllocated;
   size_t _regionBytesInUse;
   size_t _regionRealBytesInUse;
   J9::J9SegmentProvider & _systemSegmentAllocator;

   typedef TR::typed_allocator<
      TR::reference_wrapper<J9MemorySegment>,
      TR::RawAllocator
      > SystemSegmentDequeAllocator;

   std::deque<
      TR::reference_wrapper<J9MemorySegment>,
      SystemSegmentDequeAllocator
      > _systemSegments;

   typedef TR::typed_allocator<
      TR::MemorySegment,
      TR::RawAllocator
      > SegmentSetAllocator;

   std::set<
      TR::MemorySegment,
      std::less< TR::MemorySegment >,
      SegmentSetAllocator
      > _segments;

   TR::MemorySegment _freeSegmentsEmpty;
   TR::MemorySegment *_freeSegments;

   // Current active System segment from where memory might be allocated.
   // A segment with space larger than _systemSegmentSize is used for only one request,
   // and is to be released when the release method is invoked, e.g. when a TR::Region
   // goes out of scope. Thus _currentSystemSegment is not allowed to hold such segment.
   TR::reference_wrapper<J9MemorySegment> _currentSystemSegment;

   // Flag for if we record regions
   bool _recordRegions = false;
   // Counters to track regions in a compilation
   uint32_t _timestamp = 0;
   uint32_t _compilationSequenceNumber = 0;
   static uint32_t _globalCompilationSequenceNumber = 0;

   };

} // namespace J9

#endif // J9_SYSTEMSEGMENTPROVIDER_H
