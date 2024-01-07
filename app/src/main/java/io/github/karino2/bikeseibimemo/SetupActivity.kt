package io.github.karino2.bikeseibimemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.karino2.bikeseibimemo.ui.theme.BikeSeibiMemoTheme
import java.util.Date

class SetupActivity : ComponentActivity() {

    private fun writeLastUri(uri: Uri) = EntryList.writeLastUri(this, uri)

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            writeLastUri(it)
            setResult(RESULT_OK)
            finish()
        }
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            EntryList.createEmptyCsv(this, it)
            writeLastUri(it)
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeSeibiMemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Button(onClick = { createDocument.launch("BikeSeibi_${Date().time}.csv")}, modifier=Modifier.weight(1.0f).fillMaxWidth()) {
                            Text("新規csv")
                        }
                        // text/csvでは開けなかった
                        Button(onClick = {openDocument.launch(arrayOf("text/*"))}, modifier=Modifier.weight(1.0f).fillMaxWidth()) {
                            Text("既存csv")
                        }
                    }
                }
            }
        }
    }
}

