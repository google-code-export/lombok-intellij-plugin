/*
 * Copyright © 2010 Reinier Zwitserloot, Roel Spilker and Robbert Jan Grootjans.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on any field to make lombok generate delegate methods that forward the call to this field.
 * <p/>
 * Example:
 * <pre>
 *     private &#64;Delegate List&lt;String&gt; foo;
 * </pre>
 * <p/>
 * will generate for example an {@code boolean add(String)} method, which contains: {@code return foo.add(arg);}, as well as all other methods in {@code List}.
 * <p/>
 * All public instance methods of the field's type, as well as all public instance methods of all the field's type's superfields are delegated, except for all methods
 * that exist in {@link Object}, the {@code canEqual(Object)} method, and any methods that appear in types
 * that are listed in the {@code excludes} property.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Delegate {
  /**
   * Normally the type of the field is used as delegate type. However, to choose a different type to delegate, you can list one (or more) types here. Note that types with
   * type arguments can only be done as a field type. A solution for this is to create a private inner interface/class with the appropriate types extended, and possibly
   * with all methods you'd like to delegate listed, and then supply that class here. The field does not actually have to implement the type you're delegating; the
   * type listed here is used only to determine which delegate methods to generate.
   * <p/>
   * NB: All methods in {@code Object}, as well as {@code canEqual(Object other)} will never be delegated.
   */
  Class<?>[] types() default {};

  /**
   * Each method in any of the types listed here (include supertypes) will <em>not</em> be delegated.
   * <p/>
   * NB: All methods in {@code Object}, as well as {@code canEqual(Object other)} will never be delegated.
   */
  Class<?>[] excludes() default {};
}
