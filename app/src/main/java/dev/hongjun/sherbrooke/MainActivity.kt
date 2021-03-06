package dev.hongjun.sherbrooke

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import coil.compose.rememberImagePainter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dev.hongjun.sherbrooke.ui.theme.ProjetSherbrookeTheme
import kotlinx.serialization.SerializationException


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjetSherbrookeTheme {
                MainPage()
            }
        }
    }
}

@Composable
fun MainPage() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.MyProfile,
        Screen.Scan,
        Screen.History,
    )
    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.MyProfile.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.MyProfile.route) {
                MyProfile(navController)
            }
            composable(Screen.Scan.route) {
                Scan(navController)
            }
            composable(Screen.History.route) {
                History(navController)
            }
            composable(Screen.ViewProfile.route) {
                ProfileViewer(navController = navController)
            }
        }
    }
}

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object MyProfile : Screen("my_profile", R.string.my_profile)
    object Scan : Screen("scan", R.string.scan)
    object History : Screen("history", R.string.history)
    object ViewProfile : Screen("view_profile", R.string.view_profile)
}

private val dummyUserInfo = UserInfo(
    name = Name("Dummy", "Last"),
    email = Email("first.last@example.com"),
    phoneNumber = PhoneNumber("123-456-7890"),
    socialNetworks = SocialNetworks(
        discordTag = DiscordTag("tomato#0001"),
        instagramUsername = "tomato",
    ),
    notes = "Notes"
)

@Composable
fun MyProfile(navController: NavController) {
    val context = LocalContext.current
    val profile = getUserProfile(context) ?: run {
        setUserProfile(context, dummyUserInfo)
        //Toast.makeText(context, "User profile is null", Toast.LENGTH_LONG).show()
        dummyUserInfo
    }

    val bitmap = QRCodeConverter.generateQRCodeFromString(
        toJson(
            profile
        )
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = profile.name.toString(), modifier = Modifier.padding(16.dp), fontSize = 30.sp)
        Image(
            painter = rememberImagePainter(bitmap), contentDescription = null, modifier = Modifier
                .clip(shape = RoundedCornerShape(16.dp))
                .height(300.dp)
                .fillMaxSize()
                .padding(16.dp)
        )
        ProfileModifierPreview()
    }
}

@Composable
fun Scan(navController: NavController) {
    val context = LocalContext.current
    val barcodeLauncher: ActivityResultLauncher<ScanOptions> = rememberLauncherForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
        } else {

            // deserialize text into UserInfo struct
            try {
                val contents = fromJson<UserInfo>(result.contents)
                userInfo = contents
//                Toast.makeText(context, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
                navController.navigate(Screen.ViewProfile.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } catch (e: SerializationException) {
                Toast.makeText(context, "Invalid QR Code", Toast.LENGTH_LONG).show()
            }

        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Scan")
        Button(onClick = { barcodeLauncher.launch(ScanOptions()) }) {
            Text("Scanner")
        }
    }
}

// TODO: write new composable function to display user info and save to history
// TODO : A list of saved profiles, pour voir le profil, cliquez on the profile
@Composable
fun History(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "History")
        ProfileViewerPreview()
    }
}

// TODO : edits the saved user profile
@Composable
fun UserProfileEditor(navController: NavController) {

}

private var userInfo = dummyUserInfo

// TODO : shows user info
@Composable
fun ProfileViewer(navController: NavController) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        ProfileNameCard(name = userInfo.name)
        ProfileEmailCard(email = userInfo.email)
        ProfileGenericCopyingCard(
            key = stringResource(R.string.phone_number),
            value = userInfo.phoneNumber.toString()
        )
        val socialNetworks = userInfo.socialNetworks
        socialNetworks.discordTag?.let {
            ProfileGenericCopyingCard(key = "Discord", value = it.toString())
        }
        socialNetworks.instagramUsername?.let {
            ProfileGenericCopyingCard(key = "Instagram", value = it)
        }
    }
}

fun Context.copyToClipboard(text: CharSequence) =
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("label", text))


val profileInfoCardModifier = Modifier
    .fillMaxWidth()
    .padding(8.dp)
    .clickable { }

val profileInfoCardElevation = 10.dp

@Composable
fun ProfileNameCard(name: Name) {
    Card(
        modifier = profileInfoCardModifier,
        elevation = profileInfoCardElevation
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                fontSize = 15.sp,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.W900, color = Color(
                                0xFF8B98FF
                            )
                        )
                    ) {
                        append(stringResource(R.string.name))
                    }
                }
            )
            Text(
                fontSize = 22.sp,
                text = buildAnnotatedString {
                    append(name.toString())
                }
            )
        }
    }
}

@Composable
fun ProfileEmailCard(email: Email) {
    Card(
        modifier = profileInfoCardModifier,
        elevation = profileInfoCardElevation
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                fontSize = 15.sp,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.W900, color = Color(
                                0xFF8B98FF
                            )
                        )
                    ) {
                        append(stringResource(R.string.email))
                    }
                }
            )
            Text(
                fontSize = 22.sp,
                text = buildAnnotatedString {
                    append(email.toString())
                }
            )
        }
    }
}

@Composable
fun ProfileGenericCopyingCard(key: String, value: String) {
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    Card(
        modifier = profileInfoCardModifier.clickable {
            context.copyToClipboard(value)
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        },
        elevation = profileInfoCardElevation
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Text(
                fontSize = 15.sp,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.W900, color = Color(
                                0xFF8B98FF
                            )
                        )
                    ) {
                        append(key)
                    }
                }
            )
            Text(
                fontSize = 22.sp,
                text = buildAnnotatedString {
                    append(value)
                }
            )
        }
    }
}

@Composable
fun ProfileCard(userInfo: UserInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                fontSize = 22.sp,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.W900, color = Color(
                                0xFF8B98FF
                            )
                        )
                    ) {
                        append(userInfo.name.toString())
                    }
                }
            )
            Text(
                fontSize = 15.sp,
                text = buildAnnotatedString {
                    append(userInfo.phoneNumber.toString())
                }
            )
        }
    }
}

@Composable
fun TestCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                buildAnnotatedString {
                    append("welcome to ")
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.W900, color = Color(0xFF4552B8))
                    ) {
                        append("Jetpack Compose Playground")
                    }
                }
            )
            Text(
                buildAnnotatedString {
                    append("Now you are in the ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.W900)) {
                        append("Card")
                    }
                    append(" section")
                }
            )
        }
    }
}

@Composable
fun ProfileModifier(navController: NavController, hashMap: HashMap<String, String>) {

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        //ProfileNameCard(name = dummyUserInfo.name)
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.firstname))
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.name))
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.email))
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.phone_number))
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.discord_tag))
        TestModifiableCard(hashMap = hashMap, key = stringResource(id = R.string.instagram_username)
        )

        var info: UserInfo
        var bitmap = QRCodeConverter.generateQRCodeFromString(
            ""
        )


        Button(onClick = {

            try{
            info = buildUserInfo(hashMap = hashMap)
                bitmap = QRCodeConverter.generateQRCodeFromString(
                    toJson(
                        info
                    )
                )
            }
            catch(e: IllegalArgumentException){
                e.printStackTrace()
            }
            println(bitmap.toString())
            //QRCodeConverter.generateQRCodeFromString(toJson(info))


        }) {
            Text(text = "Generate QR Code")
            Image(
                painter = rememberImagePainter(bitmap), contentDescription = null, modifier = Modifier
                    .clip(shape = RoundedCornerShape(16.dp))
                    .height(300.dp)
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }



    }
}

@Composable
fun TestModifiableCard(key: String, hashMap: HashMap<String, String>) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clickable { },
        elevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            var name by rememberSaveable {
                mutableStateOf("")
            }
            TextFieldChanged(
                name = name,
                onNameChange = { name = it },
                key = key,
                hashMap = hashMap
            )


        }
    }


}

@Composable
fun TextFieldChanged(
    name: String,
    key: String,
    hashMap: HashMap<String, String>,
    onNameChange: (String) -> Unit
) {
    Text(
        fontSize = 15.sp,
        text = buildAnnotatedString {
            append(name)
        }
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(text = key) }
    )

    hashMap[key] = name
    //val context = LocalContext.current
    //Toast.makeText(context, hashMap[key], Toast.LENGTH_LONG).show()
}


fun buildUserInfo(hashMap: HashMap<String, String>): UserInfo {

    val userInfo = UserInfo(
        name = Name(hashMap["First Name"]!!, hashMap["Name"]!!),
        email = Email(hashMap["Email"]!!),
        phoneNumber = PhoneNumber(hashMap["Phone Number"]!!),
        socialNetworks = SocialNetworks(
            discordTag = DiscordTag(hashMap["Discord Tag"]!!),
            instagramUsername = hashMap["Instagram Username"]!!
        ),
        notes = ""

    )
    return userInfo

}

@Preview
@Composable
fun ProfileViewerPreview() {
    ProjetSherbrookeTheme {
        ProfileViewer(navController = rememberNavController())
    }
}

@Preview
@Composable
fun ProfileModifierPreview() {
    val hashMap = HashMap<String, String>()
    hashMap[stringResource(id = R.string.name)]
    hashMap[stringResource(id = R.string.firstname)]
    hashMap[stringResource(id = R.string.email)]
    hashMap[stringResource(id = R.string.phone_number)]
    hashMap[stringResource(id = R.string.discord_tag)]
    hashMap[stringResource(id = R.string.instagram_username)]
    //hashMap["notes"]


    ProjetSherbrookeTheme {
        ProfileModifier(navController = rememberNavController(), hashMap = hashMap)
        //TestModifiableCard()
    }
}
