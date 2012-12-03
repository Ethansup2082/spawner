package com.bergerkiller.bukkit.common.reflection;

import java.lang.reflect.Field;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;

/**
 * Wraps around the java.lang.reflect.Field class to provide an error-free alternative<br>
 * Exceptions are logged, isValid can be used to check if the Field is actually working
 * 
 * @param <T> type of the Field
 */
public class SafeField<T> extends SafeBase {
	private Field field;

	public SafeField(String fieldPath) {
		if (LogicUtil.nullOrEmpty(fieldPath) || !fieldPath.contains(".")) {
			Bukkit.getLogger().log(Level.SEVERE, "Field path contains no class: " + fieldPath);
			return;
		}
		try {
			String className = StringUtil.getBefore(fieldPath, ".");
			String methodName = fieldPath.substring(className.length() + 1);
			load(Class.forName(className), methodName);
		} catch (Throwable t) {
			System.out.println("Failed to load field '" + fieldPath + "':");
			t.printStackTrace();
		}
	}

	public SafeField(Object value, String name) {
		load(value == null ? null : value.getClass(), name);
	}

	public SafeField(Class<?> source, String name) {
		load(source, name);
	}

	private void load(Class<?> source, String name) {
		if (source == null) {
			new Exception("Can not load field '" + name + "' because the class is null!").printStackTrace();
			return;
		}
		// try to find the field
		Class<?> tmp = source;
		while (tmp != null) {
			try {
				this.field = tmp.getDeclaredField(name);
				this.field.setAccessible(true);
				return;
			} catch (NoSuchFieldException ex) {
				tmp = tmp.getSuperclass();
			} catch (SecurityException ex) {
				new Exception("No permission to access field '" + name + "' in class file '" + source.getSimpleName() + "'").printStackTrace();
				return;
			}
		}
		handleReflectionMissing("Field", name, source);
	}

	/**
	 * Checks if this Field is valid
	 * 
	 * @return True if valid, False if not
	 */
	public boolean isValid() {
		return this.field != null;
	}

	/**
	 * Transfers the value from one instance to the other
	 * 
	 * @param from instance
	 * @param to instance
	 * @return the old value of the to instance
	 */
	public T transfer(Object from, Object to) {
		if (this.field == null)
			return null;
		T old = get(to);
		set(to, get(from));
		return old;
	}

	/**
	 * Gets this Field from a certain Object
	 * 
	 * @param object to get the Field from
	 * @return the Field value, or null if not possible
	 */
	@SuppressWarnings("unchecked")
	public T get(Object object) {
		if (this.field == null)
			return null;
		try {
			return (T) this.field.get(object);
		} catch (Throwable t) {
			t.printStackTrace();
			this.field = null;
			return null;
		}
	}

	/**
	 * Sets this Field on a certain Object
	 * 
	 * @param object to set the Field on
	 * @param value to set to
	 * @return True if successful, False if not
	 */
	public boolean set(Object object, T value) {
		if (this.field == null)
			return false;
		try {
			this.field.set(object, value);
			return true;
		} catch (Throwable t) {
			t.printStackTrace();
			this.field = null;
			return false;
		}
	}

	/**
	 * Tries to set a Field for a certain Object
	 * 
	 * @param source to set a Field for
	 * @param fieldname to set
	 * @param value to set to
	 * @return True if successful, False if not
	 */
	public static <T> boolean set(Object source, String fieldname, T value) {
		return new SafeField<T>(source, fieldname).set(source, value);
	}

	/**
	 * Tries to get a Field from a certain Object
	 * 
	 * @param source to get the Field from
	 * @param fieldname to get
	 * @return The Field value, or null if not possible
	 */
	public static <T> T get(Object source, String fieldname) {
		return new SafeField<T>(source, fieldname).get(source);
	}

	/**
	 * Tries to get a static Field from a class
	 * 
	 * @param clazz to get the field value for
	 * @param fieldname of the field value
	 * @return The Field value, or null if not possible
	 */
	public static <T> T get(Class<?> clazz, String fieldname) {
		return new SafeField<T>(clazz, fieldname).get(null);
	}
}