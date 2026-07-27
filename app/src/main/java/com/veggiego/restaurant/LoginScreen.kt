package com.veggiego.restaurant

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit


@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {

    val context =
        LocalContext.current

    val auth =
        FirebaseAuth.getInstance()

    var phone by remember {
        mutableStateOf("")
    }

    var otp by remember {
        mutableStateOf("")
    }

    var verificationId by remember {
        mutableStateOf("")
    }

    var otpSent by remember {
        mutableStateOf(false)
    }

    var resendTime by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(resendTime) {
        if (resendTime > 0) {
            delay(1000)
            resendTime--
        }
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        verticalArrangement =
            Arrangement.Center

    ) {

        Text(
            text = "VeggieGo Restaurant",
            style =
                MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        if (!otpSent) {

            OutlinedTextField(

                value = phone,

                onValueChange = {
                    phone = it.filter { char ->
                        char.isDigit()
                    }.take(10)
                },

                label = {
                    Text("Phone Number")
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(

                onClick = {
                    if (phone.length != 10) {

                        Toast.makeText(
                            context,
                            "Please enter valid 10 digit mobile number",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }
                    otpSent = true
                    otp = ""
                    verificationId = ""

                    val options =

                        PhoneAuthOptions
                            .newBuilder(auth)

                            .setPhoneNumber(
                                "+91$phone"
                            )

                            .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                            )

                            .setActivity(
                                context as Activity
                            )

                            .setCallbacks(

                                object :
                                    PhoneAuthProvider
                                    .OnVerificationStateChangedCallbacks() {

                                    override fun onVerificationCompleted(
                                        credential: PhoneAuthCredential
                                    ) {
                                    }

                                    override fun onVerificationFailed(
                                        e: FirebaseException
                                    ) {

                                        otpSent = false
                                        otp = ""
                                        verificationId = ""
                                        resendTime = 0

                                        Toast.makeText(
                                            context,
                                            e.message ?: "OTP send failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    override fun onCodeSent(
                                        id: String,
                                        token:
                                        PhoneAuthProvider
                                        .ForceResendingToken
                                    ) {

                                        verificationId = id

                                        otpSent = true

                                        resendTime = 60

                                        Toast.makeText(
                                            context,
                                            "OTP Sent",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            .build()

                    PhoneAuthProvider
                        .verifyPhoneNumber(
                            options
                        )
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    "SEND OTP"
                )
            }

        } else {

            Text(
                text = "We've sent a 6-digit OTP to",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "+91 $phone",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
            OutlinedTextField(

                value = otp,

                onValueChange = {
                    otp = it.filter { char ->
                        char.isDigit()
                    }.take(6)
                },

                label = {
                    Text("Enter OTP")
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(

                onClick = {

                    if (otp.length != 6) {

                        Toast.makeText(
                            context,
                            "Please enter valid 6 digit OTP",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@Button
                    }

                    val credential =

                        PhoneAuthProvider
                            .getCredential(
                                verificationId,
                                otp
                            )

                    auth.signInWithCredential(
                        credential
                    )

                        .addOnSuccessListener {

                            onLoginSuccess(
                                phone
                            )
                        }

                        .addOnFailureListener { error ->

                            Toast.makeText(
                                context,
                                error.message
                                    ?: "OTP verification failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                },

                enabled = verificationId.isNotEmpty(),

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    if (verificationId.isEmpty())
                        "SENDING OTP..."
                    else
                        "VERIFY OTP"
                )
            }
            Spacer(
                modifier = Modifier.height(10.dp)
            )

            TextButton(

                onClick = {

                    otpSent = false

                    otp = ""

                    verificationId = ""

                    resendTime = 0

                }

            ) {

                Text("← Change Mobile Number")

            }
            TextButton(

                onClick = {

                    otp = ""

                    val options =

                        PhoneAuthOptions
                            .newBuilder(auth)

                            .setPhoneNumber(
                                "+91$phone"
                            )

                            .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                            )

                            .setActivity(
                                context as Activity
                            )

                            .setCallbacks(

                                object :
                                    PhoneAuthProvider
                                    .OnVerificationStateChangedCallbacks() {

                                    override fun onVerificationCompleted(
                                        credential: PhoneAuthCredential
                                    ) {
                                    }

                                    override fun onVerificationFailed(
                                        e: FirebaseException
                                    ) {

                                        otp = ""
                                        verificationId = ""
                                        resendTime = 0

                                        Toast.makeText(
                                            context,
                                            e.message ?: "OTP resend failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    override fun onCodeSent(
                                        id: String,
                                        token:
                                        PhoneAuthProvider
                                        .ForceResendingToken
                                    ) {

                                        verificationId = id

                                        otpSent = true

                                        resendTime = 60

                                        Toast.makeText(
                                            context,
                                            "OTP Sent",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            .build()

                    PhoneAuthProvider
                        .verifyPhoneNumber(
                            options
                        )
                },

                enabled = resendTime == 0,

                modifier = Modifier.fillMaxWidth()

            ) {

                Text(
                    if (resendTime > 0)
                        "Resend OTP in ${resendTime}s"
                    else
                        "Resend OTP"
                )
            }
        }
    }
}