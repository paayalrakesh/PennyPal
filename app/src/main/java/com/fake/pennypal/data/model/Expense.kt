package com.fake.pennypal.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude // <-- Add this import

@Entity(tableName = "expenses")
data class Expense(
    // This is the local ID for the Room database.
    // We tell Firestore to completely ignore it.
    @PrimaryKey(autoGenerate = true)
    @get:Exclude
    var id: Int = 0,

    // This is the ID from the Firestore document.
    var documentId: String = "",

    // All your other fields remain the same
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var description: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var photoUrl: String = ""
)