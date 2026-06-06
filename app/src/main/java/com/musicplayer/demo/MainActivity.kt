package com.musicplayer.demo

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private var isPlaying = false
    private var currentProgress = 35
    private var albumArtView: ImageView? = null
    private var albumArtContainer: CardView? = null
    private var playPauseBtn: ImageButton? = null
    private var slider: Slider? = null
    private var glassPanel: CardView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置状态栏透明实现全屏液态玻璃效果
        setupTransparentStatusBar()

        // 初始化视图
        initViews()

        // 设置播放按钮点击切换
        setupPlayPause()

        // 设置进度条
        setupSlider()

        // 初始化封面模糊背景
        setupGlassBackground()
    }

    private fun setupTransparentStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun initViews() {
        albumArtView = findViewById(R.id.albumArt)
        albumArtContainer = findViewById(R.id.albumArtContainer)
        playPauseBtn = findViewById(R.id.btnPlayPause)
        slider = findViewById(R.id.progressSlider)
        glassPanel = findViewById(R.id.glassPanel)
    }

    private fun setupPlayPause() {
        findViewById<View>(R.id.btnPlayPause).setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                playPauseBtn?.setImageResource(R.drawable.ic_pause)
                startPulseAnimation()
            } else {
                playPauseBtn?.setImageResource(R.drawable.ic_play_arrow)
                stopPulseAnimation()
            }
        }

        findViewById<View>(R.id.btnNext).setOnClickListener {
            // 简单演示：切换颜色动画
            rotateAlbumColors()
        }

        findViewById<View>(R.id.btnPrevious).setOnClickListener {
            // 模拟切换上一首
            animateGlassPanel()
        }
    }

    private fun setupSlider() {
        slider?.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentProgress = value.toInt()
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

    private fun rotateAlbumColors() {
        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.accent_pink),
            ContextCompat.getColor(this, R.color.accent_purple),
            ContextCompat.getColor(this, R.color.accent_blue),
        )
        val colorAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            colors[0], colors[1], colors[2], colors[0]
        )
        colorAnimator.duration = 2000
        colorAnimator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            findViewById<CardView>(R.id.playPauseContainer)
                ?.setCardBackgroundColor(color)
        }
        colorAnimator.start()
    }

    private fun animateGlassPanel() {
        glassPanel?.animate()
            ?.translationX(50f)
            ?.alpha(0.7f)
            ?.setDuration(200)
            ?.withEndAction {
                glassPanel?.animate()
                    ?.translationX(0f)
                    ?.alpha(1f)
                    ?.setDuration(200)
                    ?.start()
            }
            ?.start()
    }

    /**
     * 模拟液态玻璃背景效果：
     * 从封面提取颜色，动态更新玻璃面板色调
     */
    private fun setupGlassBackground() {
        // 用封面图提取颜色来模拟玻璃效果
        val drawable = albumArtView?.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    val vibrantColor = palette?.getVibrantColor(
                        ContextCompat.getColor(this, R.color.accent_pink)
                    )
                    val mutedColor = palette?.getMutedColor(
                        ContextCompat.getColor(this, R.color.accent_purple)
                    )
                    // 用提取的颜色更新底部玻璃面板
                    if (vibrantColor != null && mutedColor != null) {
                        animateGlassPanelColor(glassPanel, vibrantColor, mutedColor)
                    }
                }
            }
        }
    }

    private fun animateGlassPanelColor(panel: CardView?, startColor: Int, endColor: Int) {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
        animator.duration = 1500
        animator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            panel?.setCardBackgroundColor(color)
            // 调整玻璃透明度
            panel?.alpha = 0.15f
        }
        animator.start()
    }
}
