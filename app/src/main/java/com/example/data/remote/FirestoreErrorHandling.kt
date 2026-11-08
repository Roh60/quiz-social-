package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
  READ, WRITE, CREATE, UPDATE, DELETE, LIST, BATCH
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
  val code = if (error is FirebaseFirestoreException) error.code.name else "UNKNOWN"
  val structuredLog = """
    {
      "event": "firestore_error",
      "operation": "${operation.name}",
      "path": "$path",
      "errorCode": "$code",
      "message": "${error.message?.replace("\"", "\\\"")}"
    }
  """.trimIndent()
  Log.e("FirestoreError", structuredLog, error)
}
