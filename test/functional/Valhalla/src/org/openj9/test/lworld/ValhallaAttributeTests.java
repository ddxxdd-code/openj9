/*******************************************************************************
 * Copyright IBM Corp. and others 2023
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
package org.openj9.test.lworld;

import org.testng.annotations.Test;
import org.testng.Assert;

@Test(groups = { "level.sanity" })
public class ValhallaAttributeTests {

	class MultiPreloadTest {}

	/* A class may have no more than one Preload attribute. */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*Multiple Preload attributes.*")
	static public void testMultiplePreloadAttributes() throws Throwable {
		String className = MultiPreloadTest.class.getName();
		ValhallaAttributeGenerator.generateClassWithTwoPreloadAttributes("MultiPreloadAttributes",
			new String[]{className}, new String[]{className});
	}

	class PreloadClass {}

	@Test
	static public void testPreloadBehavior() throws Throwable {
		String className = PreloadClass.class.getName();
		/* Verify PreloadClass is not loaded */
		Assert.assertNull(ValhallaAttributeGenerator.findLoadedTestClass(className));
		/* Generate and load class that preloads PreloadClass */
		ValhallaAttributeGenerator.generateClassWithPreloadAttribute("PreloadBehavior", new String[]{className});
		/* Verify that PreloadClass is loaded */
		Assert.assertNotNull(ValhallaAttributeGenerator.findLoadedTestClass(className));
	}

	value class PreloadValueClass {}

	@Test
	static public void testPreloadValueClassBehavior() throws Throwable {
		String className = PreloadValueClass.class.getName();
		/* Verify PreloadValueClass is not loaded */
		Assert.assertNull(ValhallaAttributeGenerator.findLoadedTestClass(className));
		/* Generate and load class that preloads PreloadClass */
		ValhallaAttributeGenerator.generateClassWithPreloadAttribute("PreloadValueClassBehavior", new String[]{className});
		/* Verify that PreloadValueClass is loaded */
		Assert.assertNotNull(ValhallaAttributeGenerator.findLoadedTestClass(className));
	}

	/* A class may have no more than one ImplicitCreation attribute. */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*Multiple ImplicitCreation attributes.*")
	static public void testMultipleImplicitCreationAttributes() throws Throwable {
		ValhallaAttributeGenerator.generateClassWithTwoImplicitCreationAttributes("MultiImplicitCreationAttributes");
	}

	/* There must not be an ImplicitCreation attribute in the attributes table of any other ClassFile structure representing a class, interface, or module.
	 * In other words any non value class.
	 */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*The ImplicitCreation attribute is only allowed in a non-abstract value class.*")
	static public void testNonValueTypeClassWithImplicitCreationAttribute() throws Throwable {
		ValhallaAttributeGenerator.generateNonValueTypeClassWithImplicitCreationAttribute("NonValueTypeImplicitCreationAttribute");
	}

	/* ImplicitCreation smoke test. The attribute doesn't do anything in the vm right now, make sure
	 * it passes class validation.
	 */
	@Test
	static public void testValueTypeClassWithImplicitCreationAttribute() throws Throwable {
		ValhallaAttributeGenerator.generateValidClassWithImplicitCreationAttribute("ValueTypeClassWithImplicitCreationAttribute");
	}

	/* There must be no more than one NullRestricted attribute in the attributes table of a field_info structure */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*Multiple NullRestricted attributes present.*")
	static public void testMultipleNullRestrictedAttributes() throws Throwable {
		ValhallaAttributeGenerator.generateFieldWithMultipleNullRestrictedAttributes("TestMultipleNullRestrictedAttributes", "TestMultipleNullRestrictedAttributesField");
	}

	/* There must not be a NullRestricted attribute in the attributes table of a field_info structure whose descriptor_index references a primitive type. */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*A field with a primitive type cannot have a NullRestricted attribute.*")
	static public void testNullRestrictedNotAllowedInPrimitiveField() throws Throwable {
		ValhallaAttributeGenerator.generateNullRestrictedAttributeInPrimitiveField("TestNullRestrictedNotAllowedInPrimitiveField");
	}

	/* There must not be a NullRestricted attribute in the attributes table of a field_info structure whose descriptor_index references an array type */
	@Test(expectedExceptions = java.lang.ClassFormatError.class, expectedExceptionsMessageRegExp = ".*A field with an array type cannot have a NullRestricted attribute.*")
	static public void testNullRestrictedNotAllowedInArrayTypeField() throws Throwable {
		ValhallaAttributeGenerator.generateNullRestrictedAttributeInArrayField("TestNullRestrictedNotAllowedInArrayTypeField", "TestNullRestrictedNotAllowedInArrayTypeFieldField");
	}

	/* Instance field with NullRestricted attribute cannot be set to null. */
	@Test(expectedExceptions = java.lang.NullPointerException.class)
	static public void testPutFieldNullToNullRestrictedField() throws Throwable {
		Class<?> c = ValhallaAttributeGenerator.generatePutFieldNullToNullRestrictedField("TestPutFieldNullToNullRestrictedField", "TestPutFieldNullToNullRestrictedFieldField");
		c.newInstance();
	}

	/* Static field with NullRestricted attribute cannot be set to null.
	 * putstatic should throw NPE if field with NullRestricted attribute is assigned null.
	 * JVMS 5.5 says if putstatic casuses a class to initialize and fails with an exception
	 * it should be wrapped in ExceptionInInitializerError.
	 * Since value fields are implicitly final this will always be the case.
	 */
	@Test
	static public void testPutStaticNullToNullRestrictedField() throws Throwable {
		try {
			Class<?> c = ValhallaAttributeGenerator.generatePutStaticNullToNullRestrictedField("TestPutStaticNullToNullRestrictedField", "TestPutStaticNullToNullRestrictedFieldField");
			c.newInstance();
		} catch(java.lang.ExceptionInInitializerError e) {
			if (e.getCause() instanceof NullPointerException) {
				return; /* pass */
			}
			throw e;
		}
		Assert.fail("Test expected a NullPointerException wrapped in ExceptionInInitializerError.");
	}

	@Test(expectedExceptions = java.lang.NullPointerException.class)
	static public void testWithFieldStoreNullToNullRestrictedField() throws Throwable {
		Class<?> c = ValhallaAttributeGenerator.generateWithFieldStoreNullToNullRestrictedField("TestWithFieldStoreNullToNullRestrictedField", "TestWithFieldStoreNullToNullRestrictedFieldField");
		c.newInstance();
	}
}
