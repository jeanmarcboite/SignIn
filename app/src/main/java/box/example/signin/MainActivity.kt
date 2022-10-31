package box.example.signin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import box.example.signin.ui.theme.SignInTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private val auth: FirebaseAuth = Firebase.auth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authResultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // R.string.default_web_client_id is created automatically as per google-services.json,
        // though sometimes the IDE might not recognize it
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        Firebase.auth.currentUser?.apply {
            Log.d("boxxx", "firebase currentUser: ${displayName} [$email]")
            viewModel.setCurrentUser(this)
        }

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        authResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d("Boxxx", "firebaseAuthWithGoogle:" + account.email)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.e("boxxx", "Google sign in failed: " + e.message)
            }
            // }
        }
        setContent {
            SignInTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    UserProfileScreen(
                        viewModel = viewModel,
                        onSignIn = { signIn() },
                        onSignOut = {
                            //FirebaseAuth.getInstance().signOut()
                            googleSignInClient.signOut()
                            auth.signOut()
                            viewModel.setCurrentUser(null)
                        }
                    )
                }
            }
        }
    }
    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        // startActivityForResult(signInIntent, RC_SIGN_IN)

        authResultLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("boxxx", "signInWithCredential:success")
                    // val user = auth.currentUser
                    viewModel.setCurrentUser(auth.currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("boxxx", task.exception.toString())
                    // updateUI(null)
                }
            }
    }
}
@Composable
fun UserProfileScreen(viewModel: MainViewModel, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    val currentUser = viewModel.currentUser

    Column(modifier = Modifier.padding(16.dp)) {
        if (currentUser == null) {
            Button(onClick = { onSignIn() }){
                Text(text = "Sign in with Google")
            }
        }
        else {
            Text(text = "Welcome, ${currentUser.displayName} [${currentUser.email}]")
            Button(onClick = { onSignOut() } ) {
                Text(text = "Sign out")
            }
        }
    }
}
class MainViewModel : ViewModel() {
    var currentUser by mutableStateOf<FirebaseUser?>(null)
        private set
    @JvmName("assignCurrentUser")
    fun setCurrentUser(user: FirebaseUser?) {
        currentUser = user
    }
}
@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SignInTheme {
        Greeting("Android")
    }
}