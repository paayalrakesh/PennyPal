package com.fake.pennypal.data.repository

import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.data.model.Income
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun userId() = auth.currentUser?.uid ?: "anonymous"

    fun addIncome(income: Income) {
        val doc = db.collection("users").document(userId()).collection("income").document()
        doc.set(income.copy(id = doc.id))
    }

    fun addExpense(expense: Expense) {
        val doc = db.collection("users").document(userId()).collection("expenses").document()
        doc.set(expense.copy(id = doc.id))
    }

    fun getIncome(onResult: (List<Income>) -> Unit) {
        db.collection("users").document(userId()).collection("income")
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.toObjects(Income::class.java) ?: emptyList()
                onResult(data)
            }
    }

    fun getExpenses(onResult: (List<Expense>) -> Unit) {
        db.collection("users").document(userId()).collection("expenses")
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                onResult(data)
            }
    }
}
