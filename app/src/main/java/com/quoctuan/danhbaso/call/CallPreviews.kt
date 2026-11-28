package com.quoctuan.danhbaso.call

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ───────────────────────── Xem trước trong Android Studio (Preview) ─────────────────────────
// Mở file này → bấm nút "Split" góc trên bên phải. Dữ liệu dưới đây là DỮ LIỆU MẪU,
// chỉ hiện trong Android Studio, không có trong app trên điện thoại.

@Composable
private fun CallPreviewFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().callBackground().padding(top = 24.dp, bottom = 16.dp)) { content() }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Gọi đến · số chưa lưu", widthDp = 360, heightDp = 780)
@Composable
private fun PreviewIncomingUnknown() = CallPreviewFrame {
    IncomingContent(saved = null, number = "0373557834", sim = "SIM1", waiting = false, onAnswer = {}, onDecline = {}, onSpeaker = {}, onSmsReply = {})
}

@androidx.compose.ui.tooling.preview.Preview(name = "Gọi đến · người có tên", widthDp = 360, heightDp = 780)
@Composable
private fun PreviewIncomingSaved() = CallPreviewFrame {
    IncomingContent(saved = "Quốc Tiến", number = "0703426749", sim = "SIM1", waiting = false, onAnswer = {}, onDecline = {}, onSpeaker = {}, onSmsReply = {})
}

@androidx.compose.ui.tooling.preview.Preview(name = "Gọi đến · chữ Rất lớn", widthDp = 360, heightDp = 780, fontScale = 1.3f)
@Composable
private fun PreviewIncomingBigText() = CallPreviewFrame {
    IncomingContent(saved = "Mẹ Kiều", number = "0917600108", sim = "SIM1", waiting = false, onAnswer = {}, onDecline = {}, onSpeaker = {}, onSmsReply = {})
}

@androidx.compose.ui.tooling.preview.Preview(name = "Đang gọi", widthDp = 360, heightDp = 780)
@Composable
private fun PreviewOngoing() = CallPreviewFrame {
    OngoingContent(
        saved = "Mẹ Kiều", number = "0917600108", status = "02:15", active = true, held = false,
        muted = false, speaker = true, otherBar = null,
        onMute = {}, onSpeaker = {}, onHold = {}, onDtmf = {}, onAddPerson = {}, onContacts = {}, onHangup = {},
    )
}

@androidx.compose.ui.tooling.preview.Preview(name = "Gọi nhóm · đang mời", widthDp = 360, heightDp = 780)
@Composable
private fun PreviewGroupInviting() = CallPreviewFrame {
    val m = listOf(
        GroupRow("Mẹ Kiều", GroupCall.St.JOINED),
        GroupRow("Ba", GroupCall.St.RINGING),
        GroupRow("Chị Hai", GroupCall.St.WAIT),
    )
    GroupCallContent("Gia đình", m, null, false, false, {}, {}, {}, {}, {}, {})
}

@androidx.compose.ui.tooling.preview.Preview(name = "Gọi nhóm · đang nói chuyện", widthDp = 360, heightDp = 780)
@Composable
private fun PreviewGroupTalking() = CallPreviewFrame {
    val m = listOf(
        GroupRow("Mẹ Kiều", GroupCall.St.JOINED),
        GroupRow("Chị Hai", GroupCall.St.JOINED),
        GroupRow("Ba", GroupCall.St.MISSED),
    )
    GroupCallContent("Gia đình", m, "01:24", false, true, {}, {}, {}, {}, {}, {})
}

