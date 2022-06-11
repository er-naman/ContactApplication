package com.example.contactapplication;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.contactapplication.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //view bindings
    private ActivityMainBinding binding;
    //Tag
    private static final String TAG = "CONTACT TAG";
    //Write contact permission request constant
    private static final int WRITE_CONTACT_PERMISSION_CODE = 100;
    //Image pick (Gallery) intent constant
    private static final int IMAGE_PICK_GALLERY_CODE = 200;
    //Array of permission to request for write contact
    private String[]  contactPermissions;

    //image uri
    private Uri image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init permission array, need only write contact permission
        contactPermissions = new String[]{Manifest.permission.WRITE_CONTACTS};

        //thumbnailIv click, to pick image image from gallery
        binding.ThumbnailIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryIntent();
                
            }
        });

        //savecontact click, to save contact in the contact list
        binding.savecontact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isWriteContactPermissionEnabled()){
                    //permission already enabled
                    saveContact();
                }
                else{
                    //permission not enabled, request
                    requestWriteContactPermission();

                }
            }
        });
    }

    private void saveContact() {
        //input data
        String firstName = binding.firstnameET.getText().toString().trim();
        String lastName = binding.lastnameET.getText().toString().trim();
        String mobile = binding.mobileET.getText().toString().trim();
        String mobileHome = binding.phonehomeET.getText().toString().trim();
        String email = binding.emailET.getText().toString().trim();
        String address = binding.addressET.getText().toString().trim();

        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        //contact id
        int rawContactId = cpo.size();
        cpo.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        .build());

        //Add First Name , Last Name
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                        .build());

        // Add Mobile Number
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobile)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        // Add Phone(Home) Number
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobileHome)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());

        //Add E-mail
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build());

        //Add Address
        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.DATA, address)
                .withValue(ContactsContract.CommonDataKinds.SipAddress.TYPE, ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME)
                .build());

        //Get Image, convert images into bytes to store in contact
        byte[] imageBytes = imageUriToBytes();
        if (imageBytes != null){
            //Contact with image
            //Add image
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.RawContacts.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.RawContacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                    .build());
        }
        else{
            //Contact without image
        }

        //save contact
        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, cpo);
            Log.d(TAG, "saveContact: Saved... ");
            Toast.makeText(this, "Saved...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "saveContact: "+e.getMessage());
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] imageUriToBytes() {
        Bitmap bitmap;
        ByteArrayOutputStream baos = null;
        try{
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image_uri);
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            return baos.toByteArray();
        }
        catch(Exception e){
            Log.d(TAG,"imageUriToBytes: "+e.getMessage());
            return null;
        }
    }


    private void openGalleryIntent() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private boolean isWriteContactPermissionEnabled(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)==(PackageManager.PERMISSION_GRANTED);
        return  result;
    }

    private void requestWriteContactPermission(){
        ActivityCompat.requestPermissions(this,contactPermissions, WRITE_CONTACT_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //handle permission results
        if(grantResults.length > 0){
            if(requestCode == WRITE_CONTACT_PERMISSION_CODE){

                boolean haveWriteContactPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if(haveWriteContactPermission){
                    //permission granted save conatct
                    saveContact();
                }
                else{
                    //permission denied, can't save contact
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},1);
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //handle result either image picked or not
        if (resultCode==RESULT_OK){
            if(requestCode==IMAGE_PICK_GALLERY_CODE){
                //image picked
                image_uri = data.getData();
                //set to image view
                binding.ThumbnailIv.setImageURI(image_uri);
            }
        }
        else {
            //cancelled
            Toast.makeText(this, "Cancelled...", Toast.LENGTH_SHORT).show();
        }
    }
}


