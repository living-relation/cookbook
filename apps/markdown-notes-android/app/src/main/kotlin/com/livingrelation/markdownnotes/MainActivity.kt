package com.livingrelation.markdownnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.livingrelation.markdownnotes.core.markdown.MarkdownEngine
import com.livingrelation.markdownnotes.feature.reader.ReadScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sample =
            """
            # Welcome

            This app was built by an automated pipeline.

            | Feature | Status |
            | --- | --- |
            | Read | Ready |
            | Edit | In progress |
            """
                .trimIndent()
        val document = MarkdownEngine().parse(sample)
        setContent {
            MaterialTheme {
                Surface {
                    ReadScreen(document = document)
                }
            }
        }
    }
}
