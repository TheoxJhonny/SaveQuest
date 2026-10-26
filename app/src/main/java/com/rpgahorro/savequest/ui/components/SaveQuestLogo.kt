package com.rpgahorro.savequest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rpgahorro.savequest.R

/** Transparent wordmark shared by the app's brand headers. */
@Composable
fun SaveQuestLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.savequest_logo),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.aspectRatio(3f),
        contentScale = ContentScale.Fit,
    )
}
