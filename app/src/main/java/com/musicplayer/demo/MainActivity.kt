package com.musicplayer.demo

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette
import eightbitlab.com.blurview.BlurView
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private var isPlaying = false
    private var isLiked = false
    private lateinit var blurTarget: View
    private lateinit var glassPanel: BlurView
    private var albumArtContainer: CardView? = null
    private var playPauseBtn: ImageButton? = null
    private var playPauseContainer: CardView? = null
    private var slider: Slider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupTransparentStatusBar()
        initViews()
        setupBlurView()
        setupPlayPause()
        setupSlider()
        setupBackgroundFromAlbumArt()
    }

    private fun setupTransparentStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun initViews() {
        blurTarget = findViewById(R.id.blurTarget)
        glassPanel = findViewById(R.id.glassPanel)
        albumArtContainer = findViewById(R.id.albumArtContainer)
        playPauseBtn = findViewById(R.id.btnPlayPause)
        playPauseContainer = findViewById(R.id.playPauseContainer)
        slider = findViewById(R.id.progressSlider)
    }

    /**
     * 设置 BlurView 的 iOS 风格实时模糊：
     * 1. 从 BlurTarget 获取快照
     * 2. 底层内容实时高斯模糊
     * 3. 加上半透明覆盖色 (blurOverlayColor) 实现液态玻璃质感
     */
    private fun setupBlurView() {
        // 获取窗口背景作为 frame clear drawable
        // 这样即使根布局有透明区域，BlurView 也不会变得半透明
        val windowBackground: Drawable? = window.decorView.background

        // 设置模糊半径 (iOS 风格通常 15-25)
        val blurRadius = 20f

        glassPanel.setupWith(blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(blurRadius)

        // 圆角裁剪 — 让玻璃面板四角圆润
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            glassPanel.outlineProvider = ViewOutlineProvider.BACKGROUND
            glassPanel.clipToOutline = true
        }
    }

    private fun setupPlayPause() {
        findViewById<View>(R.id.btnPlayPause).setOnClickListener {
            isPlaying = !isPlaying
            playPauseBtn?.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            )
            if (isPlaying) startPulseAnimation() else stopPulseAnimation()
        }

        findViewById<View>(R.id.btnNext).setOnClickListener {
            rotatePlayButtonColors()
        }

        findViewById<View>(R.id.btnPrevious).setOnClickListener {
            // 触感反馈：短暂滑动动画
            glassPanel.animate()
                .translationX(50f)
                .alpha(0.7f)
                .setDuration(150)
                .withEndAction {
                    glassPanel.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }

        findViewById<View>(R.id.btnQueue).setOnClickListener {
            // 收藏按钮交互
            isLiked = !isLiked
            findViewById<ImageButton>(R.id.btnQueue).animate()
                .scaleX(if (isLiked) 1.3f else 1.0f)
                .scaleY(if (isLiked) 1.3f else 1.0f)
                .setDuration(200)
                .start()
        }
    }

    private fun setupSlider() {
        slider?.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                // 拖动进度时更新 UI 反馈
            }
        }
    }

    private fun startPulseAnimation() {
        albumArtContainer?.animate()
            ?.scaleX(1.05f)
            ?.scaleY(1.05f)
            ?.setDuration(1500)
            ?.withEndAction {
                albumArtContainer?.animate()
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.setDuration(1500)
                    ?.withEndAction { startPulseAnimation() }
                    ?.start()
            }
            ?.start()
    }

    private fun stopPulseAnimation() {
        albumArtContainer?.animate()?.cancel()
        albumArtContainer?.scaleX = 1.0f
        albumArtContainer?.scaleY = 1.0f
    }

    /**
     * 播放按钮颜色流动 — 模拟专辑色彩流转
     */
    private fun rotatePlayButtonColors() {
        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.accent_pink),
            ContextCompat.getColor(this, R.color.accent_purple),
            ContextCompat.getColor(this, R.color.accent_blue),
        )
        val animator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            colors[0], colors[1], colors[2], colors[0]
        )
        animator.duration = 2000
        animator.addUpdateListener {
            playPauseContainer?.setCardBackgroundColor(it.animatedValue as Int)
        }
        animator.start()
    }

    /**
     * 从专辑封面提取颜色，用于设置玻璃面板的覆盖色
     */
    private fun setupBackgroundFromAlbumArt() {
        val albumArtView = findViewById<ImageView>(R.id.albumArt)
        val drawable = albumArtView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    val vibrant = palette?.getVibrantColor(
                        ContextCompat.getColor(this, R.color.accent_pink)
                    )
                    // 将来可用于动态调整玻璃面板的覆盖色
                }
            }
        }
    }
}
