package app.dayquest.ui.components

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dayquest.R
import app.dayquest.ui.theme.D
import app.dayquest.ui.theme.DType

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "dayquest"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
fun CommunityLinks() {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    SectionTitle(stringResource(R.string.community_title))
    DCard {
        Text(stringResource(R.string.community_sub), style = DType.Muted, modifier = Modifier.padding(bottom = 4.dp))
        for ((icon, label, sub, url) in listOf(
            Link("🐞", R.string.community_bug, R.string.community_bug_sub, Community.BUG),
            Link("💡", R.string.community_idea, R.string.community_idea_sub, Community.IDEA),
            Link("🔔", R.string.community_follow, R.string.community_follow_sub, Community.FOLLOW),
            Link("💬", R.string.community_chat, R.string.community_chat_sub, Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 18.sp, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(label), style = DType.Strong)
                    Text(stringResource(sub), style = DType.Muted)
                }
                Text("›", style = DType.Strong.copy(fontSize = 22.sp, color = D.c.accent))
            }
        }
    }
}

private data class Link(val icon: String, @param:StringRes val label: Int, @param:StringRes val sub: Int, val url: String)
