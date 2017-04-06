package com.nvidia.developer.opengl.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FieldControl {

	public static final int CALL_ANY = 0;
	public static final int CALL_FIELD = 1;
	public static final int CALL_METHOD = 2;

	Field field;
	Method setter;
	Method getter;
	Object ower;
	final int index;
	final int callType;

	public FieldControl(Object obj, String fieldName, Class<?>... args) {
		this(obj, -1, fieldName, 0, args);
	}

	public FieldControl(Object obj, String fieldName, int callType,
			Class<?>... args) {
		this(obj, -1, fieldName, callType, args);
	}
	
	public FieldControl(Object obj, int index, String fieldName,  Class<?>... args) {
		this(obj, index, fieldName, 0, args);
	}

	public FieldControl(Object obj, int index,  String fieldName, int callType,
			Class<?>... args) {
		this.ower = obj;
		this.callType = callType;
		this.index = index;

		init(fieldName, args);
	}

	protected void init(String fieldName, Class<?>... args) {
		field = NvUtils.getField(ower, fieldName);
//		if (field == null)
//			return;
		if(callType == 0 || callType == 1)
			field.setAccessible(true);
		
		if(callType == 1){
			return;
		}

		String[] names = NvUtils.generateGetterAndSetterName(fieldName);
		getter = NvUtils.getMethod(ower, names[0]);
		setter = NvUtils.getMethod(ower, names[1], args);
	}

	public void setValue(Object value) {
		try {
			_setter(value);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private final void _setter(Object value) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		switch (callType) {
		case CALL_ANY:
			if (setter != null) {
				setter.invoke(ower, value);
			} else {
				if (field != null) {
					if(index >= 0){
						Array.set(field.get(ower), index, value);
					}else{
					    field.set(ower, value);
					}
				}
			}
			break;

		case CALL_FIELD:
			if (field != null) {
				if(index >= 0){
					Array.set(field.get(ower), index, value);
				}else{
				    field.set(ower, value);
				}
			}
			break;
		case CALL_METHOD:
			if (setter != null) {
				setter.invoke(ower, value);
			}
			break;
		default:
			System.err.printf("Unkown callType: %d.\n", callType);
		}
	}

	private final Object _getter() throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		switch (callType) {
		case CALL_ANY:
			if (getter != null) {
				return getter.invoke(ower);
			} else {
				if (field != null) {
					Object v = field.get(ower);
					if(index >= 0){
						return Array.get(v, index);
					}else
						return v;
				}
			}
			break;

		case CALL_FIELD:
			if (field != null) {
				Object v = field.get(ower);
				if(index >= 0){
					return Array.get(v, index);
				}else
					return v;
			}
			break;
		case CALL_METHOD:
			if (getter != null) {
				return getter.invoke(ower);
			}
			break;
		default:
			System.err.printf("Unkown callType: %d.\n", callType);
		}
		return null;
	}

	public Object getValue() {
		try {
			return _getter();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}
}
