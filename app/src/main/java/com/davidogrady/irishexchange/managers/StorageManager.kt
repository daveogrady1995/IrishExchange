package com.davidogrady.irishexchange.managers

import com.google.firebase.storage.FirebaseStorage
import java.util.*

class StorageManager(private val mStorage: FirebaseStorage) {

    fun uploadImageToFirebaseStorage(compressedImageByteArray: ByteArray, uploadImageSuccessHandler: (fileLocation: String) -> Unit,
                                     uploadImageErrorHandler: (message: String) -> Unit) {
        val filename = UUID.randomUUID().toString()
        val ref = mStorage.getReference("images/$filename")
        ref.putBytes(compressedImageByteArray).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener {
                uploadImageSuccessHandler.invoke(it.toString())
            }
            ref.downloadUrl.addOnFailureListener {
                uploadImageErrorHandler(it.message!!)
            }
        }.addOnFailureListener {
            uploadImageErrorHandler(it.message!!)
        }
    }

    fun deleteImageInFirebaseStorage(selectedPhotoUrl: String, deleteImageCompleteHandler: () -> Unit) {
        val photoRef = mStorage.getReferenceFromUrl(selectedPhotoUrl)
        photoRef.delete().addOnCompleteListener {
            deleteImageCompleteHandler.invoke()
        }
    }
}