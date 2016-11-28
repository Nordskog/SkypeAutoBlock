package com.nordskog.skypeautoblock;

import java.lang.reflect.Method;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class LoadPackageHook implements IXposedHookLoadPackage
{ 

		private static Object waitingAuthorizationEnum = null;
		private static Object getContactGroupEnum()
		{
			return waitingAuthorizationEnum;
		}
	
		private static void hookContactRequest(LoadPackageParam lpparam) throws NoSuchMethodException, XposedHelpers.ClassNotFoundError
		{
			final Class contactClassRequestNotificationManagerClass = XposedHelpers.findClass("com.skype.android.app.contacts.ContactRequestNotificationManager", lpparam.classLoader);
			
			
			final Class contactClass = XposedHelpers.findClass("com.skype.Contact", lpparam.classLoader);
			final Class conversationClass = XposedHelpers.findClass("com.skype.Conversation", lpparam.classLoader);
			
			final Class conversationImplClass = XposedHelpers.findClass("com.skype.ConversationImpl", lpparam.classLoader);
			
			final Class contactGroupTypeClass = XposedHelpers.findClass("com.skype.ContactGroup.TYPE", lpparam.classLoader);
			
			{
				Object[] enums = contactGroupTypeClass.getEnumConstants();
				
				for (Object currentEnum : enums)
				{
					if (currentEnum.toString().equals("CONTACTS_WAITING_MY_AUTHORIZATION"))
					{
						waitingAuthorizationEnum = currentEnum;
					}
				}
					
			}		
			
			final Method Contact_isMemberOfHardwiredGroupMethod = contactClass.getDeclaredMethod("isMemberOfHardwiredGroup", contactGroupTypeClass);
			final Method Contact_ignoreAuthRequestMethod = contactClass.getDeclaredMethod("ignoreAuthRequest");
			final Method Contact_setBlockedMethod = contactClass.getDeclaredMethod("setBlocked", boolean.class, boolean.class);
			final Method Contact_openConversation = contactClass.getDeclaredMethod("openConversation", conversationClass);
			
			final Method ConversationImpl_revemoFromInbox = conversationImplClass.getDeclaredMethod("removeFromInbox");
			final Method ConversationImpl_unPin = conversationImplClass.getDeclaredMethod("unPin");
			
			
			
			XposedHelpers.findAndHookMethod(contactClassRequestNotificationManagerClass, "handleNotificationForContactRequest", contactClass, conversationClass, String.class, new XC_MethodHook()
			{
		
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable
	            {
	            	
	                Object contactInstance = param.args[0];
	                //Object conversationInstance = param.args[1];
	                //String stringInstance = (String) param.args[2];
	    	
	               // Log.e("###", "Notification request!");
	                
	                if ( (boolean) Contact_isMemberOfHardwiredGroupMethod.invoke(contactInstance, getContactGroupEnum() ))
	                {
	                	
	                	//Log.e("###", "Was unauthorized!");
	                	 
	                	
	                	{
	                		//Stuff done when you deny a contact request
	                		Contact_ignoreAuthRequestMethod.invoke(contactInstance, (Object[]) null);
	                		Contact_setBlockedMethod.invoke(contactInstance, true,true );
	                		
	                		//Stuff done by util class, method g
	                		Object ConversationImplInstance = conversationImplClass.newInstance();
	                		Contact_openConversation.invoke(contactInstance, ConversationImplInstance );
	                		ConversationImpl_revemoFromInbox.invoke(ConversationImplInstance, (Object[]) null);
	                		ConversationImpl_unPin.invoke(ConversationImplInstance, (Object[]) null);	
	                	}
	                	
	                	//XposedBridge.log("Blocked a contact request!");

	                	Log.i("SkypeAutoBlock", "Blocked a contact request.");
	                	
	                	param.setResult(null);
	                }
	                //else
	                	//Log.e("###", "Was NOT unauthorized!");
	                
	            }
	
			});
			
		}
	

		@Override
	    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable 
	    {
	        if ( !(lpparam.packageName.equals("com.skype.raider") ) )
	            return;
	        	
	        //Log.e("###", "We are in: "+lpparam.packageName);
	        
	        try 
	        {
	        	 hookContactRequest(lpparam);
	        }
	        catch (Exception ex)
	        {
	        	XposedBridge.log("Something went wrong hooking SkypeAutoBlock. Check logcat for stacktrace.");
	        	ex.printStackTrace();
	        }
	        
	        
	       
	    }

}
