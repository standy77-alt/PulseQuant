package com.example.rhythmtrader

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlin.concurrent.thread
import kotlin.math.abs
import com.example.rhythmtrader.ui.AppTheme
import com.example.rhythmtrader.data.PreferencesManager
import com.example.rhythmtrader.ui.SettingsDialog
import com.example.rhythmtrader.ui.UiHelpers
import com.example.rhythmtrader.ui.HomeView
import com.example.rhythmtrader.ui.BottomNavBar
import com.example.rhythmtrader.ui.CoinSelectorDialog
import com.example.rhythmtrader.ui.StrategySelectorDialog
import com.example.rhythmtrader.ui.SweepReportDialog

class MainActivity : Activity() {

    private var isLightTheme = false
    private var currentTab = 0

    private lateinit var scrollRoot: ScrollView
    private lateinit var tvAppTitle: TextView
    private lateinit var btnSettings: TextView

    private lateinit var navTabBar: LinearLayout
    private lateinit var btnTabRadar: Button
    private lateinit var btnTabTrades: Button
    private lateinit var radarTabContainer: LinearLayout
    private lateinit var tradesTabContainer: LinearLayout

    private lateinit var segmentedBar: LinearLayout
    private lateinit var heroCard: LinearLayout
    private lateinit var quantCard: LinearLayout
    private lateinit var botCard: LinearLayout
    private lateinit var settingsCard: LinearLayout
    private lateinit var deskSummaryCard: LinearLayout

    private lateinit var tvCoinTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDeltaBadge: TextView
    private lateinit var tvFairValueBand: TextView
    private lateinit var tvLastTrade: TextView
    private lateinit var radarView: RadarView

    private lateinit var tvCvdBuy: TextView
    private lateinit var tvCvdDelta: TextView
    private lateinit var tvCvdSell: TextView
    private lateinit var cvdProgressBar: ProgressBar

    private lateinit var tvQuantStateMetrics: TextView
    private lateinit var tvQuantDecisionGate: TextView
    private lateinit var tvQuantCalibration: TextView
    private lateinit var btnKillSwitch: Button

    private lateinit var tvBotBalance: TextView
    private lateinit var tvBotPnlBadge: TextView
    private lateinit var tvBotWinrate: TextView
    private lateinit var tvDecayWarning: TextView
    private lateinit var lblAutoBot: TextView
    private lateinit var tvActivePosInfo: TextView
    private lateinit var btnStrategyToggle: Button
    private lateinit var btnRunSweep: Button
    private lateinit var btnClosePos: Button
    private lateinit var btnResetDemo: TextView

    private lateinit var tvDeskBotsReport: TextView
    private lateinit var tvOpenPositionsHeader: TextView
    private lateinit var openPositionsListContainer: LinearLayout
    private var lastRenderedOpenKeys: List<String> = emptyList()
    private var lastRenderedClosedCount: Int = -1
    private var lastTradesTextUpdateMs: Long = 0L
    private val openPosPnlViews = HashMap<String, TextView>()
    private val openPosDetailViews = HashMap<String, TextView>()
    private lateinit var tvClosedTradesHeader: TextView
    private lateinit var closedTradesListContainer: LinearLayout

    private lateinit var tvSettingsHeader: TextView
    private lateinit var lblPct: TextView
    private lateinit var tvThreshVal: TextView
    private lateinit var seekPrice: SeekBar
    private lateinit var lblWhale: TextView
    private lateinit var tvWhaleVal: TextView
    private lateinit var seekWhale: SeekBar

    private lateinit var btnToggle: Button
    private lateinit var feedHeader: TextView
    private lateinit var feedContainer: LinearLayout

    private val slotButtons = mutableListOf<Button>()
    private val slotCoins = mutableListOf("BTC-USDT", "ETH-USDT", "SOL-USDT", "ZEC-USDT", "SUI-USDT")
    private val popularCoins = listOf(
        "BTC-USDT", "ETH-USDT", "SOL-USDT", "ZEC-USDT", "SUI-USDT",
        "BICO-USDT", "CGPT-USDT", "MON-USDT", "XRP-USDT", "DOGE-USDT",
        "PEPE-USDT", "TON-USDT", "ADA-USDT", "AVAX-USDT", "LINK-USDT"
    )

    private var ignoreSeekChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RadarService.loadBotStats(this)
        val (loadedTheme, loadedCoins) = PreferencesManager.load(this, slotCoins)
        isLightTheme = loadedTheme
        loadedCoins.forEachIndexed { i, c -> if (i < slotCoins.size) slotCoins[i] = c }
        AppTheme.isLight = isLightTheme

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val fontRegular = Typeface.create("sans-serif", Typeface.NORMAL)
        val fontMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val fontBold = Typeface.create("sans-serif-medium", Typeface.BOLD)
        val fontMono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        scrollRoot = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 60)
        }
        scrollRoot.addView(root)

        setupHeader(root, fontBold)

        setupTabs(root, fontBold)

        setupCoinSlots(fontBold)


        setupHeroCard(fontRegular, fontMedium, fontBold)


        setupQuantCard(fontRegular, fontBold, fontMono)

        setupBotCard(fontRegular, fontMedium, fontBold)

        setupSettingsCard(fontRegular, fontMedium, fontBold)

        setupToggleButton(fontBold)

        setupAlertFeed(fontMedium)

        setupTradesTab(fontRegular, fontBold, fontMono)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AppTheme.bgMain())
            addView(scrollRoot, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(BottomNavBar.create(this@MainActivity, 0) { index ->
                when (index) {
                    0 -> { 
                        navTabBar.visibility = View.GONE
                        switchTab(0) 
                    }
                    1 -> Toast.makeText(this@MainActivity, "Аналитика скоро", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this@MainActivity, "Стратегии скоро", Toast.LENGTH_SHORT).show()
                    3 -> SettingsDialog.show(this@MainActivity, { isLightTheme = AppTheme.isLight; applyTheme() }, { RadarService.resetBotAccount(this@MainActivity); updateBotUi() })
                }
            })
        }
        setContentView(mainContainer)
        applyTheme()
        selectCoin(RadarService.selectedChartSymbol)
        updateToggleBtn()
        updateBotUi()
        renderAlertFeed()
        renderTradesTab()
    }

    private fun setupHeader(root: LinearLayout, fontBold: Typeface) {
        // Шапка
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 20)
        }
        tvAppTitle = TextView(this).apply {
            text = "PulseQuant"
            textSize = 20f
            typeface = fontBold
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(tvAppTitle)

        btnSettings = TextView(this).apply {
            setOnClickListener {
                SettingsDialog.show(
                    context = this@MainActivity,
                    onThemeChanged = {
                        isLightTheme = AppTheme.isLight
                        PreferencesManager.saveTheme(this@MainActivity, isLightTheme)
                        applyTheme()
                    },
                    onResetAccount = {
                        RadarService.resetBotAccount(this@MainActivity)
                        updateBotUi()
                        lastRenderedOpenKeys = emptyList()
        lastRenderedClosedCount = -1
        if (currentTab == 1) renderTradesTab()
                    }
                )
            }
            text = "⚙"
            textSize = 12f
            setTextColor(AppTheme.textPrimary())
            gravity = Gravity.CENTER
            setPadding(24, 20, 24, 20)
        }
        topBar.addView(btnSettings)
        root.addView(topBar)
    }

    private fun setupTabs(root: LinearLayout, fontBold: Typeface) {
        // ПЕРЕКЛЮЧАТЕЛЬ ВКЛАДОК
        navTabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 10)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        btnTabRadar = Button(this).apply {
            text = "Радар"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            gravity = Gravity.CENTER
            setPadding(12, 14, 12, 14)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 8 }
            setOnClickListener { switchTab(0) }
        }
        btnTabTrades = Button(this).apply {
            text = "Сделки"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            gravity = Gravity.CENTER
            setPadding(12, 14, 12, 14)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(1) }
        }
        navTabBar.addView(btnTabRadar)
        navTabBar.addView(btnTabTrades)
        root.addView(navTabBar)
        navTabBar.visibility = View.GONE

        radarTabContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(radarTabContainer)

        tradesTabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        root.addView(tradesTabContainer)
    }

    private fun setupCoinSlots(fontBold: Typeface) {
        // ВКЛАДКА 1: Слоты монет
        segmentedBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        slotButtons.clear()
        for (i in slotCoins.indices) {
            val btn = Button(this).apply {
                textSize = 12f
                typeface = fontBold
                isAllCaps = false
                stateListAnimator = null
                minHeight = 0
                minimumHeight = 0
                gravity = Gravity.CENTER
                setPadding(4, 22, 4, 22)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i < slotCoins.size - 1) rightMargin = 6
                }
                setOnClickListener { selectCoin(slotCoins[i]) }
                setOnLongClickListener {
                    CoinSelectorDialog.show(
                        context = this@MainActivity,
                        slotIndex = i,
                        currentSym = slotCoins[i],
                        popularCoins = popularCoins,
                        isActive = RadarService.activeSymbols.contains(slotCoins[i])
                    ) { newSym, makeActive ->
                        applyNewCoinForSlot(i, newSym, makeActive)
                    }
                    true
                }
            }
            slotButtons.add(btn)
            segmentedBar.addView(btn)
        }
        radarTabContainer.addView(segmentedBar)
    }

    private fun setupHeroCard(fontRegular: Typeface, fontMedium: Typeface, fontBold: Typeface) {
        heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }

        val heroTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tvCoinTitle = TextView(this).apply {
            text = "ZEC / USDT"
            textSize = 12f
            typeface = fontMedium
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnResetBase = TextView(this).apply {
            text = "Сброс базы"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = fontMedium
            background = UiHelpers.createAppleCard(AppTheme.accent(), 16f, Color.TRANSPARENT)
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                RadarService.resetBasePrice(RadarService.selectedChartSymbol)
                radarView.invalidate()
                Toast.makeText(this@MainActivity, "База, CVD и паузы сброшены", Toast.LENGTH_SHORT).show()
            }
        }
        heroTopRow.addView(tvCoinTitle)
        heroTopRow.addView(btnResetBase)
        heroCard.addView(heroTopRow)

        val priceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, 8, 0, 4)
        }
        tvPrice = TextView(this).apply {
            text = "---.--"
            textSize = 24f
            typeface = fontBold
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvDeltaBadge = TextView(this).apply {
            text = "+0.00%"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = fontMedium
            background = UiHelpers.createAppleCard(AppTheme.greenDeep(), 24f, Color.TRANSPARENT)
            setPadding(22, 10, 22, 10)
        }
        priceRow.addView(tvPrice)
        priceRow.addView(tvDeltaBadge)
        heroCard.addView(priceRow)

        tvFairValueBand = TextView(this).apply {
            text = "ANALYST Fair Value: [ --- .. --- ] • Оценка: FAIR"
            textSize = 12f
            setTextColor(AppTheme.secondary())
            typeface = fontBold
            setPadding(0, 4, 0, 4)
        }
        heroCard.addView(tvFairValueBand)

        tvLastTrade = TextView(this).apply {
            text = "Ожидание потока сделок..."
            textSize = 12f
            typeface = fontRegular
            setPadding(0, 0, 0, 20)
        }
        heroCard.addView(tvLastTrade)

        radarView = RadarView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400).apply {
                bottomMargin = 12
            }
        }
        heroCard.addView(radarView)

        val cvdLabelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 10)
        }
        tvCvdBuy = TextView(this).apply {
            text = "Пок: $0K"
            textSize = 12f
            setTextColor(AppTheme.greenDeep())
            typeface = fontBold
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvCvdDelta = TextView(this).apply {
            text = "CVD: $0K"
            textSize = 12f
            typeface = fontBold
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvCvdSell = TextView(this).apply {
            text = "Прод: $0K"
            textSize = 12f
            setTextColor(AppTheme.redDeep())
            typeface = fontBold
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        cvdLabelRow.addView(tvCvdBuy)
        cvdLabelRow.addView(tvCvdDelta)
        cvdLabelRow.addView(tvCvdSell)
        heroCard.addView(cvdLabelRow)

        cvdProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 50
            progressDrawable = createCvdDrawable()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16)
        }
        heroCard.addView(cvdProgressBar)
        radarTabContainer.addView(HomeView.createBotsGrid(this))
        radarTabContainer.addView(heroCard)
    }

    private fun setupQuantCard(fontRegular: Typeface, fontBold: Typeface, fontMono: Typeface) {
        quantCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        val quantTitle = TextView(this).apply {
            text = "6-BOT DESK · COMPUTE"
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            typeface = fontBold
            setPadding(0, 0, 0, 14)
        }
        quantCard.addView(quantTitle)

        tvQuantStateMetrics = TextView(this).apply {
            text = "Секстиль: S1 | Волат: 0 bps/hr | Плечо: 8x\nVWAP Δ: 0.00% | Z: 0.00σ | Режим: BALANCED"
            textSize = 12f
            typeface = fontRegular
            setPadding(14, 10, 14, 10)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        quantCard.addView(tvQuantStateMetrics)

        tvQuantDecisionGate = TextView(this).apply {
            text = "SKEPTIC & RISK: Ожидание сигнала..."
            textSize = 12f
            setTextColor(AppTheme.greenDeep())
            typeface = fontBold
            setPadding(4, 4, 4, 10)
        }
        quantCard.addView(tvQuantDecisionGate)

        tvQuantCalibration = TextView(this).apply {
            text = "MFE Capture: 65% | Скептик убил: 0 | Спас: $0"
            textSize = 11f
            typeface = fontRegular
            setPadding(4, 0, 4, 16)
        }
        quantCard.addView(tvQuantCalibration)

        btnRunSweep = Button(this).apply {
            text = "AUTO-OPTIMIZE: ПРОВЕРКА 5 СТРАТЕГИЙ"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.purpleDeep(), 16f, AppTheme.purpleLight())
            setPadding(20, 26, 20, 26)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 14
            }
            setOnClickListener { triggerAutoAdaptation(RadarService.selectedChartSymbol) }
        }
        quantCard.addView(btnRunSweep)

        btnKillSwitch = Button(this).apply {
            text = "EMERGENCY KILL SWITCH"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.redVeryDark(), 16f, AppTheme.red())
            setPadding(14, 14, 14, 14)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
            val turningOn = !QuantEngine.killSwitchEngaged
            QuantEngine.killSwitchEngaged = turningOn
            updateBotUi()
            if (turningOn) {
                startService(Intent(this@MainActivity, RadarService::class.java).apply { action = "KILL_SWITCH" })
            }
                updateBotUi()
            }
        }
        quantCard.addView(btnKillSwitch)
        radarTabContainer.addView(quantCard)
    }

    private fun setupBotCard(fontRegular: Typeface, fontMedium: Typeface, fontBold: Typeface) {
        botCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }

        val botTopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val botTitle = TextView(this).apply {
            text = "PM Desk • Управление Ботом"
            textSize = 12f
            setTextColor(AppTheme.greenDeep())
            typeface = fontBold
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnResetDemo = TextView(this).apply {
            text = "Сброс $1000"
            textSize = 11f
            typeface = fontMedium
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                RadarService.resetBotAccount(this@MainActivity)
                updateBotUi()
                renderTradesTab()
                radarView.invalidate()
                Toast.makeText(this@MainActivity, "Демо-счет и архив сделок сброшены", Toast.LENGTH_SHORT).show()
            }
        }
        botTopRow.addView(botTitle)
        botTopRow.addView(btnResetDemo)
        botCard.addView(botTopRow)

        val balRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, 10, 0, 4)
        }
        tvBotBalance = TextView(this).apply {
            text = "$1,000.00"
            textSize = 24f
            typeface = fontBold
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvBotPnlBadge = TextView(this).apply {
            text = "PnL: +$0.00"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = fontMedium
            background = UiHelpers.createAppleCard(AppTheme.greenDeep(), 12f, Color.TRANSPARENT)
            setPadding(20, 8, 20, 8)
        }
        balRow.addView(tvBotBalance)
        balRow.addView(tvBotPnlBadge)
        botCard.addView(balRow)

        tvBotWinrate = TextView(this).apply {
            text = "Сделок: 0 (W: 0 / L: 0) • В работе: 0/3"
            textSize = 12f
            typeface = fontRegular
            setPadding(0, 0, 0, 12)
        }
        botCard.addView(tvBotWinrate)

        tvDecayWarning = TextView(this).apply {
            text = "⚠️ JOURNAL ALERT: Зафиксировано угасание стратегии! Смени постулат или запусти Sweep."
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = fontBold
            background = UiHelpers.createAppleCard(AppTheme.orangeDeep(), 12f, Color.TRANSPARENT)
            setPadding(24, 16, 24, 16)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 14
            }
        }
        tvDecayWarning.setOnClickListener { triggerAutoAdaptation(RadarService.selectedChartSymbol) }
        botCard.addView(tvDecayWarning)

        val autoBotRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 14)
        }
        lblAutoBot = TextView(this).apply {
            text = "Авто-вход (TP/SL 1:2, MFE)"
            textSize = 12f
            typeface = fontRegular
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val switchBot = Switch(this).apply {
            isChecked = RadarService.botEnabled
            setOnCheckedChangeListener { _, checked ->
                RadarService.botEnabled = checked
                RadarService.saveBotStats(this@MainActivity)
            }
        }
        autoBotRow.addView(lblAutoBot)
        autoBotRow.addView(switchBot)
        botCard.addView(autoBotRow)

        btnStrategyToggle = Button(this).apply {
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.accent(), 16f, Color.TRANSPARENT)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 16
            }
            setOnClickListener { StrategySelectorDialog.show(this@MainActivity) { updateBotUi() } }
        }
        botCard.addView(btnStrategyToggle)

        tvActivePosInfo = TextView(this).apply {
            text = "Нет открытой позиции по выбранной монете"
            textSize = 12f
            typeface = fontMedium
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 18
            }
            setOnClickListener { switchTab(1) }
        }
        botCard.addView(tvActivePosInfo)

        val manualBtnsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val btnLong = Button(this).apply {
            text = "LONG"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.greenDark(), 16f, AppTheme.greenLight())
            setPadding(12, 24, 12, 24)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 12 }
            setOnClickListener {
                startService(Intent(this@MainActivity, RadarService::class.java).apply { action = "MANUAL_LONG" })
            }
        }
        val btnShort = Button(this).apply {
            text = "SHORT"
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.redDark(), 16f, AppTheme.redLight())
            setPadding(12, 24, 12, 24)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 12 }
            setOnClickListener {
                startService(Intent(this@MainActivity, RadarService::class.java).apply { action = "MANUAL_SHORT" })
            }
        }
        btnClosePos = Button(this).apply {
            text = "ЗАКРЫТЬ"
            textSize = 11f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = UiHelpers.createAppleCard(AppTheme.grayButton(), 16f, AppTheme.grayButtonStroke())
            setPadding(12, 24, 12, 24)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                startService(Intent(this@MainActivity, RadarService::class.java).apply { action = "CLOSE_POS" })
            }
        }
        manualBtnsRow.addView(btnLong)
        manualBtnsRow.addView(btnShort)
        manualBtnsRow.addView(btnClosePos)
        botCard.addView(manualBtnsRow)
        radarTabContainer.addView(botCard)
    }

    private fun setupSettingsCard(fontRegular: Typeface, fontMedium: Typeface, fontBold: Typeface) {
        settingsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }

        tvSettingsHeader = TextView(this).apply {
            text = "Операционная поверхность • ZEC"
            textSize = 12f
            setTextColor(AppTheme.accent())
            typeface = fontBold
            setPadding(0, 0, 0, 20)
        }
        settingsCard.addView(tvSettingsHeader)

        val rowPct = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        lblPct = TextView(this).apply {
            text = "TP/SL / Z-триггер"
            textSize = 12f
            typeface = fontRegular
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvThreshVal = TextView(this).apply {
            text = "0.35%"
            textSize = 12f
            typeface = fontMedium
        }
        rowPct.addView(lblPct)
        rowPct.addView(tvThreshVal)
        settingsCard.addView(rowPct)

        seekPrice = SeekBar(this).apply {
            max = 100
            setPadding(0, 14, 0, 24)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (ignoreSeekChange) return
                    val v = if (p < 1) 1 else p
                    val pct = v / 100.0
                    tvThreshVal.text = String.format("%.2f%%", pct)
                    val sym = RadarService.selectedChartSymbol
                    val curCfg = RadarService.getConfig(this@MainActivity, sym)
                    RadarService.saveConfig(this@MainActivity, sym, pct, curCfg.whaleUsdt)
                    radarView.invalidate()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        settingsCard.addView(seekPrice)

        val rowWhale = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        lblWhale = TextView(this).apply {
            text = "Институциональный ордер (Кит)"
            textSize = 12f
            typeface = fontRegular
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvWhaleVal = TextView(this).apply {
            text = "$25K"
            textSize = 12f
            typeface = fontMedium
        }
        rowWhale.addView(lblWhale)
        rowWhale.addView(tvWhaleVal)
        settingsCard.addView(rowWhale)

        seekWhale = SeekBar(this).apply {
            max = 250
            setPadding(0, 14, 0, 20)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (ignoreSeekChange) return
                    val v = if (p < 5) 5 else p
                    val whale = v * 1000.0
                    tvWhaleVal.text = "$${v}K"
                    val sym = RadarService.selectedChartSymbol
                    val curCfg = RadarService.getConfig(this@MainActivity, sym)
                    RadarService.saveConfig(this@MainActivity, sym, curCfg.thresholdPercent, whale)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        settingsCard.addView(seekWhale)

        radarTabContainer.addView(settingsCard)
    }

    private fun setupToggleButton(fontBold: Typeface) {
        btnToggle = Button(this).apply {
            textSize = 12f
            typeface = fontBold
            isAllCaps = false
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 32
            }
            setOnClickListener { toggleService() }
        }
        radarTabContainer.addView(btnToggle)
    }

    private fun setupAlertFeed(fontMedium: Typeface) {
        feedHeader = TextView(this).apply {
            text = "Лента рыночных импульсов (Сигналы Scout)"
            textSize = 12f
            typeface = fontMedium
            setPadding(8, 0, 8, 14)
        }
        radarTabContainer.addView(feedHeader)

        feedContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        radarTabContainer.addView(feedContainer)
    }

    private fun setupTradesTab(fontRegular: Typeface, fontBold: Typeface, fontMono: Typeface) {
        // ВКЛАДКА 2: СДЕЛКИ И DESK
        deskSummaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        val deskTitle = TextView(this).apply {
            text = "СВОДКА АУДИТА"
            textSize = 12f
            setTextColor(AppTheme.textSecondary())
            typeface = fontBold
            setPadding(0, 0, 0, 12)
        }
        tvDeskBotsReport = TextView(this).apply {
            textSize = 12f
            typeface = fontRegular
            setLineSpacing(6f, 1f)
        }
        deskSummaryCard.addView(deskTitle)
        deskSummaryCard.addView(tvDeskBotsReport)
        tradesTabContainer.addView(deskSummaryCard)

        tvOpenPositionsHeader = TextView(this).apply {
            text = "🟢 АКТИВНЫЕ ПОЗИЦИИ В РАБОТЕ (0/3)"
            textSize = 12f
            typeface = fontBold
            setPadding(8, 4, 8, 14)
        }
        tradesTabContainer.addView(tvOpenPositionsHeader)

        openPositionsListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 12
            }
        }
        tradesTabContainer.addView(openPositionsListContainer)

        tvClosedTradesHeader = TextView(this).apply {
            text = "🏁 АРХИВ ЗАВЕРШЕННЫХ СДЕЛОК (0)"
            textSize = 12f
            typeface = fontBold
            setPadding(8, 4, 8, 14)
        }
        tradesTabContainer.addView(tvClosedTradesHeader)

        closedTradesListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        tradesTabContainer.addView(closedTradesListContainer)
    }

    private fun switchTab(tabIdx: Int) {
        currentTab = tabIdx
        if (tabIdx == 0) {
            radarTabContainer.visibility = View.VISIBLE
            tradesTabContainer.visibility = View.GONE
        } else {
            radarTabContainer.visibility = View.GONE
            tradesTabContainer.visibility = View.VISIBLE
            renderTradesTab()
        }
        updateTabButtonsStyle()
    }

    private fun updateTabButtonsStyle() {
        val openCount = RadarService.openPositions.size
        val closedCount = RadarService.closedTradesHistory.size
        btnTabTrades.text = "Сделки ($openCount / $closedCount)"

        if (currentTab == 0) {
            btnTabRadar.background = UiHelpers.createAppleCard(AppTheme.accent(), 16f, Color.TRANSPARENT)
            btnTabRadar.setTextColor(Color.WHITE)
            btnTabTrades.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 16f, Color.TRANSPARENT)
            btnTabTrades.setTextColor(AppTheme.textPrimary())
        } else {
            btnTabTrades.background = UiHelpers.createAppleCard(AppTheme.accent(), 16f, Color.TRANSPARENT)
            btnTabTrades.setTextColor(Color.WHITE)
            btnTabRadar.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 16f, Color.TRANSPARENT)
            btnTabRadar.setTextColor(AppTheme.textPrimary())
        }
    }

    private fun renderTradesTab() {
        updateTabButtonsStyle()

        val openPnlSum = RadarService.openPositions.values.sumOf { p ->
            val last = RadarService.lastPrices[p.symbol] ?: p.entryPrice
            p.calcUnrealizedPnl(last)
        }
        val equity = RadarService.demoBalance + openPnlSum
        val killedTotal = QuantEngine.chaseBlockedCount + QuantEngine.h2FlaggedOrdersCount

        tvDeskBotsReport.text = String.format(
            "• PM Капитал: $%.2f (Реализовано: %+.2f$)\n" +
            "• SCOUT: Активно %d потоков | Сигналов: %d\n" +
            "• SKEPTIC: Убил %d опасных входов (Спас ~$%.0f)\n" +
            "• RISK: Лимит 3 поз. | Таймер: 60м | MFE Capture: %.0f%%\n" +
            "• JOURNAL: Побед %d / Стопов %d | Статус: %s",
            equity, RadarService.totalRealizedPnl,
            RadarService.activeSymbols.size, RadarService.alertHistory.size,
            killedTotal, QuantEngine.skepticSavedUsdtEstimate,
            QuantEngine.getCaptureRatioPct(),
            RadarService.winsCount, RadarService.lossesCount,
            if (QuantEngine.isStrategyDecaying()) "⚠️ DECAY" else "OK ✓"
        )

        val positions = RadarService.openPositions.values.toList()
        val currentOpenKeys = positions.map { "${it.symbol}_${it.isLong}_${it.openTimeMs}" }
        val nowMs = System.currentTimeMillis()
        tvOpenPositionsHeader.text = "🟢 АКТИВНЫЕ ПОЗИЦИИ В РАБОТЕ (${positions.size}/3)"

        // Перестраиваем View ТОЛЬКО если открылась/закрылась позиция!
        if (currentOpenKeys != lastRenderedOpenKeys) {
            lastRenderedOpenKeys = currentOpenKeys
            openPositionsListContainer.removeAllViews()
            openPosPnlViews.clear()
            openPosDetailViews.clear()

            if (positions.isEmpty()) {
                val emptyTv = TextView(this).apply {
                    text = "Сейчас нет открытых сделок. Когда бот (или ты вручную) откроет позицию, она появится здесь с живым расчетом прибыли."
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary())
                    setPadding(8, 12, 8, 20)
                }
                openPositionsListContainer.addView(emptyTv)
            } else {
                for (pos in positions) {
                    val curP = RadarService.lastPrices[pos.symbol] ?: pos.entryPrice
                    val uPnl = pos.calcUnrealizedPnl(curP)
                    val curBps = pos.currentExcursionBps(curP) - QuantEngine.ROUNDTRIP_FEE_BPS
                    val isPos = uPnl >= 0
                    val holdMin = ((nowMs - pos.openTimeMs) / 60000L).toInt()
                    val shortSym = pos.symbol.replace("-USDT", "")

                    val posCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = UiHelpers.createAppleCard(
                            AppTheme.bgCard(),
                            16f,
                            if (isPos) AppTheme.greenDeep() else AppTheme.redDeep()
                        )
                        setPadding(32, 28, 32, 28)
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = 20
                        }
                    }

                    val topRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val sideTitle = if (pos.isLong) "LONG 🟢" else "SHORT 🔴"
                    val tvSym = TextView(this).apply {
                        text = "$shortSym  $sideTitle  ($${pos.marginUsdt.toInt()} × ${pos.leverage}x)"
                        textSize = 12f
                        setTextColor(AppTheme.textPrimary())
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val tvPnl = TextView(this).apply {
                        text = String.format("%+.2f$ (%+.0f bps)", uPnl, curBps)
                        textSize = 12f
                        setTextColor(Color.WHITE)
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        background = UiHelpers.createAppleCard(
                            if (isPos) AppTheme.greenDeep() else AppTheme.redDeep(),
                            12f, Color.TRANSPARENT
                        )
                        setPadding(10, 4, 10, 4)
                    }
                    openPosPnlViews[pos.symbol] = tvPnl
                    topRow.addView(tvSym)
                    topRow.addView(tvPnl)
                    posCard.addView(topRow)

                    val lockBadge = if (pos.mfeLockActivated) "🔒 Стоп в БУ (+15 bps)" else "TP/SL 1:2"
                    val details = TextView(this).apply {
                        text = "Вход: ${pos.entryPrice}  →  Сейчас: $curP\n" +
                               "TP: ${String.format("%.2f", pos.takeProfitPrice)} | SL: ${String.format("%.2f", pos.stopLossPrice)} ($lockBadge)\n" +
                               "Открыта в ${pos.openTime} (в рынке ${holdMin}/60 мин) • Пик MFE: +${pos.maxFavorableExcursionBps.toInt()} bps"
                        textSize = 12f
                        setTextColor(AppTheme.textSecondary())
                        setPadding(0, 12, 0, 16)
                        setLineSpacing(5f, 1f)
                    }
                    openPosDetailViews[pos.symbol] = details
                    posCard.addView(details)

                    val actRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }
                    val btnGoChart = Button(this).apply {
                        text = "На график"
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        isAllCaps = false
                        stateListAnimator = null
                        minHeight = 0
                        minimumHeight = 0
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background = UiHelpers.createAppleCard(AppTheme.accent(), 24f, Color.TRANSPARENT)
                        setPadding(16, 24, 16, 24)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = 12 }
                        setOnClickListener {
                            selectCoin(pos.symbol)
                            switchTab(0)
                            radarView.invalidate()
                        }
                    }
                    val btnCloseItem = Button(this).apply {
                        text = "Закрыть"
                        textSize = 12f
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        isAllCaps = false
                        stateListAnimator = null
                        minHeight = 0
                        minimumHeight = 0
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background = UiHelpers.createAppleCard(AppTheme.redDark(), 24f, Color.TRANSPARENT)
                        setPadding(16, 24, 16, 24)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener {
                            isEnabled = false
                            text = "Закрытие..."
                            startService(Intent(this@MainActivity, RadarService::class.java).apply {
                                action = "CLOSE_POS"
                                putExtra("target_symbol", pos.symbol)
                            })
                        }
                    }
                    actRow.addView(btnGoChart)
                    actRow.addView(btnCloseItem)
                    posCard.addView(actRow)

                    openPositionsListContainer.addView(posCard)
                }
            }
        } else if (nowMs - lastTradesTextUpdateMs >= 250L) {
            // Обновляем только цифры внутри карточек (4 раза в секунду), НЕ трогая кнопки!
            lastTradesTextUpdateMs = nowMs
            for (pos in positions) {
                val curP = RadarService.lastPrices[pos.symbol] ?: pos.entryPrice
                val uPnl = pos.calcUnrealizedPnl(curP)
                val curBps = pos.currentExcursionBps(curP) - QuantEngine.ROUNDTRIP_FEE_BPS
                val isPos = uPnl >= 0
                val holdMin = ((nowMs - pos.openTimeMs) / 60000L).toInt()
                val lockBadge = if (pos.mfeLockActivated) "🔒 Стоп в БУ (+15 bps)" else "TP/SL 1:2"

                openPosPnlViews[pos.symbol]?.apply {
                    text = String.format("%+.2f$ (%+.0f bps)", uPnl, curBps)
                    background = UiHelpers.createAppleCard(
                        if (isPos) AppTheme.greenDeep() else AppTheme.redDeep(),
                        12f, Color.TRANSPARENT
                    )
                }
                openPosDetailViews[pos.symbol]?.text =
                    "Вход: ${pos.entryPrice}  →  Сейчас: $curP\n" +
                    "TP: ${String.format("%.2f", pos.takeProfitPrice)} | SL: ${String.format("%.2f", pos.stopLossPrice)} ($lockBadge)\n" +
                    "Открыта в ${pos.openTime} (в рынке ${holdMin}/60 мин) • Пик MFE: +${pos.maxFavorableExcursionBps.toInt()} bps"
            }
        }

        val closedList = RadarService.closedTradesHistory
        tvClosedTradesHeader.text = "🏁 АРХИВ ЗАВЕРШЕННЫХ СДЕЛОК (${closedList.size})"

        if (closedList.size != lastRenderedClosedCount) {
            lastRenderedClosedCount = closedList.size
            closedTradesListContainer.removeAllViews()

            if (closedList.isEmpty()) {
                val emptyClosed = TextView(this).apply {
                    text = "Завершенных сделок пока нет. Здесь будут храниться все закрытые сделки с подробным расчетом профита."
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary())
                    setPadding(8, 12, 8, 20)
                }
                closedTradesListContainer.addView(emptyClosed)
            } else {
                for (tr in closedList) {
                    val won = tr.netPnlUsdt >= 0
                    val shortSym = tr.symbol.replace("-USDT", "")
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = UiHelpers.createAppleCard(AppTheme.bgCard(), 16f, Color.TRANSPARENT)
                        setPadding(28, 22, 28, 22)
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = 14
                        }
                    }

                    val r1 = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val icon = if (won) "✅" else "❌"
                    val side = if (tr.isLong) "LONG" else "SHORT"
                    val tTitle = TextView(this).apply {
                        text = "$icon $shortSym $side ($${tr.marginUsdt.toInt()}×${tr.leverage}x)"
                        textSize = 12f
                        setTextColor(AppTheme.textPrimary())
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val tPnl = TextView(this).apply {
                        text = String.format("%+.2f$ (%+.0f bps)", tr.netPnlUsdt, tr.netBps)
                        textSize = 12f
                        setTextColor(if (won) AppTheme.greenDeep() else AppTheme.redDeep())
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    }
                    r1.addView(tTitle)
                    r1.addView(tPnl)
                    card.addView(r1)

                    val tSub = TextView(this).apply {
                        text = "Вход: ${tr.entryPrice} (${tr.openTime}) → Выход: ${tr.exitPrice} (${tr.closeTime})\n" +
                               "${tr.reason} • Макс. пик MFE: +${tr.mfeBps.toInt()} bps"
                        textSize = 12f
                        setTextColor(AppTheme.textSecondary())
                        setPadding(0, 8, 0, 0)
                        setLineSpacing(4f, 1f)
                    }
                    card.addView(tSub)

                    closedTradesListContainer.addView(card)
                }
            }
        }
    }

    private fun triggerAutoAdaptation(symbol: String) {
        if (RadarService.isAutoAdapting) return
        RadarService.isAutoAdapting = true
        btnRunSweep.isEnabled = false
        btnRunSweep.text = "⏳ АВТООПТИМИЗАЦИЯ: $symbol..."
        tvDecayWarning.visibility = View.VISIBLE
        tvDecayWarning.text = "🔄 Проверяю стратегии и устойчивость результатов для $symbol..."

        thread {
            try {
                val report = BacktestEngine.runMultiStrategyAutoAdapt(
                    symbol, RadarService.demoBalance
                )
                runOnUiThread {
                    RadarService.isAutoAdapting = false
                    btnRunSweep.isEnabled = true
                    btnRunSweep.text = "AUTO-OPTIMIZE: ПРОВЕРКА 5 СТРАТЕГИЙ"

                    val cell = report.bestCell
                    val shortSym = symbol.replace("-USDT", "")

                    if (cell.verdict != "DATA VALIDATED") {
                        RadarService.autoTradeAuthorized = false
                        RadarService.authorizedStrategyMode = -1
                        val summary =
                            "⚠️ NO EDGE [$shortSym]: надёжного победителя нет. Текущие настройки сохранены."
                        RadarService.lastAutoAdaptSummary = summary
                        tvDecayWarning.background = UiHelpers.createAppleCard(
                            AppTheme.orangeVeryDark(), 12f, Color.TRANSPARENT
                        )
                        tvDecayWarning.text = summary
                        return@runOnUiThread
                    }

                    val newStrat = report.bestStrategyMode
                    RadarService.botStrategyMode = newStrat
                    RadarService.authorizedStrategyMode = newStrat
                    RadarService.autoTradeAuthorized = true

                    val cfg = RadarService.getConfig(this@MainActivity, symbol)
                    cfg.thresholdPercent = cell.thresholdPct
                    RadarService.saveConfig(
                        this@MainActivity, symbol,
                        cfg.thresholdPercent, cfg.whaleUsdt
                    )
                    RadarService.saveBotStats(this@MainActivity)

                    btnStrategyToggle.text =
                        "🔬 ${QuantEngine.getStrategyShortTitle(newStrat)} (Выбрать из 5)"
                    selectCoin(RadarService.selectedChartSymbol)

                    val summary =
                        "🤖 AUTO-OPTIMIZED [$shortSym]: " +
                        "${QuantEngine.getStrategyShortTitle(newStrat)} • " +
                        "Порог ${String.format("%.2f%%", cell.thresholdPct)} • " +
                        "OOS ${String.format("%+.0f bps", cell.oosNetBps)} (${cell.verdict})"
                    RadarService.lastAutoAdaptSummary = summary
                    tvDecayWarning.background = UiHelpers.createAppleCard(
                        AppTheme.greenDeep(), 12f, Color.TRANSPARENT
                    )
                    tvDecayWarning.text = summary
                }
            } catch (e: Exception) {
                runOnUiThread {
                    RadarService.isAutoAdapting = false
                    btnRunSweep.isEnabled = true
                    btnRunSweep.text = "AUTO-OPTIMIZE: ПРОВЕРКА 5 СТРАТЕГИЙ"
                    tvDecayWarning.text = "❌ Автооптимизация не завершена: ${e.message ?: "ошибка"}"
                }
            }
        }
    }

    private fun executeParameterSweep() {
        val sym = RadarService.selectedChartSymbol
        val stratMode = RadarService.botStrategyMode
        btnRunSweep.isEnabled = false
        btnRunSweep.text = "⏳ Загрузка 500 свечей $sym и прогон 20 матриц..."

        thread {
            val report = BacktestEngine.runFullSweep(sym, stratMode, RadarService.demoBalance)
            runOnUiThread {
                btnRunSweep.isEnabled = true
                btnRunSweep.text = "AUTO-OPTIMIZE: ПРОВЕРКА 5 СТРАТЕГИЙ"
                SweepReportDialog.show(this@MainActivity, report) { sym -> selectCoin(sym) }
            }
        }
    }

    private fun applyTheme() {
        AppTheme.isLight = isLightTheme
        scrollRoot.setBackgroundColor(AppTheme.bgMain())
        tvAppTitle.setTextColor(AppTheme.textPrimary())
        btnSettings.setTextColor(AppTheme.textPrimary())



        navTabBar.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
        segmentedBar.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
        heroCard.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
        quantCard.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.accent())
        botCard.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
        settingsCard.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
        deskSummaryCard.background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.accent())

        tvCoinTitle.setTextColor(AppTheme.textSecondary())
        tvPrice.setTextColor(AppTheme.textPrimary())
        tvLastTrade.setTextColor(AppTheme.textSecondary())
        tvCvdDelta.setTextColor(AppTheme.textPrimary())

        tvQuantStateMetrics.setTextColor(AppTheme.textPrimary())
        tvQuantStateMetrics.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 24f, AppTheme.stroke())
        tvQuantCalibration.setTextColor(AppTheme.textSecondary())

        btnResetDemo.setTextColor(AppTheme.textPrimary())
        btnResetDemo.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 12f, AppTheme.stroke())
        tvBotBalance.setTextColor(AppTheme.textPrimary())
        tvBotWinrate.setTextColor(AppTheme.textSecondary())
        lblAutoBot.setTextColor(AppTheme.textPrimary())
        tvActivePosInfo.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 16f, AppTheme.stroke())

        tvDeskBotsReport.setTextColor(AppTheme.textPrimary())
        tvOpenPositionsHeader.setTextColor(AppTheme.textPrimary())
        tvClosedTradesHeader.setTextColor(AppTheme.textPrimary())

        lblPct.setTextColor(AppTheme.textPrimary())
        tvThreshVal.setTextColor(AppTheme.textSecondary())
        lblWhale.setTextColor(AppTheme.textPrimary())
        tvWhaleVal.setTextColor(AppTheme.textSecondary())
        feedHeader.setTextColor(AppTheme.textSecondary())

        findViewById<LinearLayout>(9999)?.let { BottomNavBar.updateColors(this, it, 0) }
        radarView.isLightTheme = isLightTheme
        updateTabButtonsStyle()
        updateSegmentedStyles()
        updateBotUi()
        renderAlertFeed()
        lastRenderedOpenKeys = emptyList()
        lastRenderedClosedCount = -1
        if (currentTab == 1) renderTradesTab()
    }

    private fun updateBotUi() {
        val sym = RadarService.selectedChartSymbol
        val curPrice = RadarService.lastPrices[sym] ?: 0.0
        val pos = RadarService.openPositions[sym]

        val snap = QuantEngine.latestSnapshots[sym]
        if (snap != null) {
            tvFairValueBand.text = String.format(
                "ANALYST Fair Value: [ %.2f .. %.2f ] • %s",
                snap.fairLow, snap.fairHigh, snap.valuationTag
            )
            tvQuantStateMetrics.text = String.format(
                "Секстиль: S%d | Волат: %.0f bps/hr | Плечо: %dx\nVWAP Δ: %+.2f%% | Z: %+.2fσ | Режим: %s",
                snap.volSextile, snap.volBpsPerHour, snap.recommendedLeverage,
                snap.moveFromVwapPct, snap.zScore, snap.regime
            )
        }
        val dec = QuantEngine.latestDecisions[sym]
        if (dec != null) {
            tvQuantDecisionGate.text = "SKEPTIC & RISK: ${dec.reason}"
        }
        val totalKills = QuantEngine.chaseBlockedCount + QuantEngine.h2FlaggedOrdersCount
        tvQuantCalibration.text = String.format(
            "MFE Capture: %.0f%% | Скептик убил: %d | Спас от убытков: ~$%.0f",
            QuantEngine.getCaptureRatioPct(), totalKills, QuantEngine.skepticSavedUsdtEstimate
        )

        tvDecayWarning.visibility = if (QuantEngine.isStrategyDecaying()) View.VISIBLE else View.GONE

        if (QuantEngine.killSwitchEngaged) {
            btnKillSwitch.text = "KILL SWITCH АКТИВЕН"
            btnKillSwitch.background = UiHelpers.createAppleCard(AppTheme.red(), 16f, Color.WHITE)
        } else {
            btnKillSwitch.text = "EMERGENCY KILL SWITCH"
            btnKillSwitch.background = UiHelpers.createAppleCard(AppTheme.redVeryDark(), 16f, AppTheme.red())
        }

        val openPnlSum = RadarService.openPositions.values.sumOf { p ->
            val last = RadarService.lastPrices[p.symbol] ?: p.entryPrice
            p.calcUnrealizedPnl(last)
        }
        val equity = RadarService.demoBalance + openPnlSum
        tvBotBalance.text = String.format("$%.2f", equity)

        val totalPnl = RadarService.totalRealizedPnl + openPnlSum
        val isPos = totalPnl >= 0
        tvBotPnlBadge.text = String.format("PnL: %+.2f$", totalPnl)
        tvBotPnlBadge.setTextColor(Color.WHITE)
        tvBotPnlBadge.background = UiHelpers.createAppleCard(
            if (isPos) AppTheme.greenDeep() else AppTheme.redDeep(),
            12f, Color.TRANSPARENT
        )

        val totalTrades = RadarService.winsCount + RadarService.lossesCount
        tvBotWinrate.text = "Сделок: $totalTrades (W: ${RadarService.winsCount} / L: ${RadarService.lossesCount}) • В работе: ${RadarService.openPositions.size}/3"

        val sTitle = QuantEngine.getStrategyShortTitle(RadarService.botStrategyMode)
        btnStrategyToggle.text = "$sTitle (Выбрать из 5)"

        if (pos != null && curPrice > 0) {
            val uPnl = pos.calcUnrealizedPnl(curPrice)
            val curBps = pos.currentExcursionBps(curPrice)
            val side = if (pos.isLong) "LONG 🟢" else "SHORT 🔴"
            val lockTag = if (pos.mfeLockActivated) "🔒 БУ АКТИВЕН" else "TP/SL 1:2"
            tvActivePosInfo.text = String.format(
                "%s %s ($%.0f × %dx) • %s\nPnL: %+.2f$ (%+.0f bps) | Нажми для вкладки Сделок ➔",
                pos.symbol.replace("-USDT", ""), side, pos.marginUsdt, pos.leverage, lockTag,
                uPnl, curBps
            )
            tvActivePosInfo.setTextColor(AppTheme.textPrimary())
        } else {
            tvActivePosInfo.text = "По ${sym.replace("-USDT", "")} нет открытой позиции (Нажми, чтобы открыть все сделки ➔)"
            tvActivePosInfo.setTextColor(AppTheme.textSecondary())
        }

        updateTabButtonsStyle()
    }

    private fun selectCoin(sym: String) {
        RadarService.selectedChartSymbol = sym
        if (!RadarService.activeSymbols.contains(sym)) {
            RadarService.activeSymbols.add(sym)
            if (RadarService.isRunning) {
                startService(Intent(this, RadarService::class.java).apply { action = "RESUB" })
            }
        }
        val shortName = sym.replace("-USDT", "")
        tvCoinTitle.text = "$shortName / USDT  •  BingX Perp"
        tvSettingsHeader.text = "Операционная поверхность • $shortName"

        val cfg = RadarService.getConfig(this, sym)
        ignoreSeekChange = true
        seekPrice.progress = (cfg.thresholdPercent * 100).toInt()
        tvThreshVal.text = String.format("%.2f%%", cfg.thresholdPercent)
        seekWhale.progress = (cfg.whaleUsdt / 1000.0).toInt()
        tvWhaleVal.text = "$${(cfg.whaleUsdt / 1000).toInt()}K"
        ignoreSeekChange = false

        val lastP = RadarService.lastPrices[sym]
        tvPrice.text = lastP?.toString() ?: "---.--"
        updateCvdUi(sym)
        updateBotUi()
        radarView.clearChart()
        updateSegmentedStyles()
    }

    private fun updateCvdUi(sym: String) {
        val buy = RadarService.cvdBuy[sym] ?: 0.0
        val sell = RadarService.cvdSell[sym] ?: 0.0
        val total = buy + sell
        val delta = buy - sell

        tvCvdBuy.text = "Пок: ${formatUsd(buy)}"
        tvCvdSell.text = "Прод: ${formatUsd(sell)}"
        val sign = if (delta >= 0) "+" else "-"
        tvCvdDelta.text = "CVD: $sign${formatUsd(abs(delta))}"
        tvCvdDelta.setTextColor(AppTheme.textPrimary())

        cvdProgressBar.progress = if (total > 0) ((buy / total) * 100).toInt() else 50
    }

    private fun formatUsd(v: Double): String {
        return when {
            v >= 1_000_000 -> String.format("$%.2fM", v / 1_000_000.0)
            v >= 1_000 -> String.format("$%.0fK", v / 1_000.0)
            else -> String.format("$%.0f", v)
        }
    }

    private fun renderAlertFeed() {
        feedContainer.removeAllViews()
        val items = RadarService.alertHistory
        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Ожидание сигналов от Scout & Skeptic..."
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
                gravity = Gravity.CENTER
                background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
                setPadding(32, 44, 32, 44)
            }
            feedContainer.addView(empty)
            return
        }

        for (item in items.take(15)) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = UiHelpers.createAppleCard(AppTheme.bgCard(), 24f, AppTheme.stroke())
                setPadding(28, 22, 28, 22)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 12
                }
                setOnClickListener { selectCoin(item.symbol) }
            }

            val dotColor = when (item.type) {
                1, 3 -> AppTheme.greenDeep()
                else -> AppTheme.redDeep()
            }

            val dot = View(this).apply {
                background = UiHelpers.createAppleCard(dotColor, 12f, Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(20, 20).apply { rightMargin = 24 }
            }
            card.addView(dot)

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val t1 = TextView(this).apply {
                text = item.title
                textSize = 12f
                setTextColor(AppTheme.textPrimary())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            val t2 = TextView(this).apply {
                text = item.subtitle
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
            }
            textCol.addView(t1)
            textCol.addView(t2)
            card.addView(textCol)

            val tTime = TextView(this).apply {
                text = item.time
                textSize = 12f
                setTextColor(AppTheme.textSecondary())
            }
            card.addView(tTime)

            feedContainer.addView(card)
        }
    }

    private fun applyNewCoinForSlot(slotIndex: Int, rawInput: String, makeActive: Boolean) {
        var newSym = rawInput.trim().uppercase()
        if (newSym.isEmpty()) return
        if (!newSym.contains("-")) newSym = "$newSym-USDT"

        val oldSym = slotCoins[slotIndex]
        RadarService.activeSymbols.remove(oldSym)
        slotCoins[slotIndex] = newSym

        if (makeActive || RadarService.activeSymbols.isEmpty()) {
            RadarService.activeSymbols.add(newSym)
        }

        PreferencesManager.saveCoins(this, slotCoins)
        selectCoin(newSym)

        if (RadarService.isRunning) {
            startService(Intent(this, RadarService::class.java).apply { action = "RESUB" })
        }
    }

    private fun updateSegmentedStyles() {
        for (i in slotButtons.indices) {
            val sym = slotCoins[i]
            val btn = slotButtons[i]
            val hasPos = RadarService.openPositions.containsKey(sym)
            val shortName = sym.replace("-USDT", "")
            btn.text = if (hasPos) "$shortName •" else shortName

            val isSelected = sym == RadarService.selectedChartSymbol
            val isActive = RadarService.activeSymbols.contains(sym)
            when {
                isSelected -> {
                    btn.background = UiHelpers.createAppleCard(AppTheme.accent(), 16f, Color.TRANSPARENT)
                    btn.setTextColor(Color.WHITE)
                }
                isActive -> {
                    btn.background = UiHelpers.createAppleCard(AppTheme.bgSubCard(), 16f, Color.TRANSPARENT)
                    btn.setTextColor(if (hasPos) AppTheme.greenDeep() else AppTheme.textPrimary())
                }
                else -> {
                    btn.background = UiHelpers.createAppleCard(Color.TRANSPARENT, 16f, Color.TRANSPARENT)
                    btn.setTextColor(AppTheme.textSecondary())
                }
            }
        }
    }

    private fun toggleService() {
        val intent = Intent(this, RadarService::class.java)
        if (RadarService.isRunning) {
            intent.action = "STOP"
            startService(intent)
            RadarService.isRunning = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            RadarService.isRunning = true
        }
        updateToggleBtn()
    }

    private fun updateToggleBtn() {
        if (RadarService.isRunning) {
            btnToggle.text = "🛑 Остановить 6-Bot Desk"
            btnToggle.background = UiHelpers.createAppleCard(AppTheme.redDark(), 24f, AppTheme.redLight())
            btnToggle.setTextColor(Color.WHITE)
        } else {
            btnToggle.text = "Запустить"
            btnToggle.background = UiHelpers.createAppleCard(AppTheme.blueDeep(), 24f, AppTheme.blueLight())
            btnToggle.setTextColor(Color.WHITE)
        }
    }

    override fun onResume() {
        super.onResume()
        updateToggleBtn()
        updateBotUi()
        renderAlertFeed()
        lastRenderedOpenKeys = emptyList()
        lastRenderedClosedCount = -1
        if (currentTab == 1) renderTradesTab()
        RadarService.uiCallback = { symbol, price, vol, isMaker, newAlert ->
            if (symbol == RadarService.selectedChartSymbol && price > 0) {
                tvPrice.text = price.toString()
                val base = RadarService.basePrices[symbol] ?: price
                val diff = ((price - base) / base) * 100.0
                val isPos = diff >= 0
                tvDeltaBadge.text = String.format("%+.2f%%", diff)
                tvDeltaBadge.setTextColor(Color.WHITE)
                tvDeltaBadge.background = UiHelpers.createAppleCard(
                    if (isPos) AppTheme.greenDeep() else AppTheme.redDeep(),
                    24f, Color.TRANSPARENT
                )

                val sideStr = if (!isMaker) "Покупка" else "Продажа"
                tvLastTrade.text = "Посл. сделка: ${formatUsd(vol)} ($sideStr)  •  База: $base"
                updateCvdUi(symbol)
                radarView.addTick(price, vol, !isMaker)
            }
            updateBotUi()
            updateSegmentedStyles()
            if (currentTab == 1) {
                renderTradesTab()
            }
            if (newAlert != null) {
                renderAlertFeed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        RadarService.uiCallback = null
    }

    private fun createSliderDrawable(): LayerDrawable {
        val bg = GradientDrawable().apply {
            setColor(if (AppTheme.isLight) Color.parseColor("#E7ECF2") else Color.parseColor("#232E42"))
            cornerRadius = 12f
        }
        val startColor = if (AppTheme.isLight) Color.parseColor("#99BDF2") else Color.parseColor("#A3C7F7")
        val endColor = if (AppTheme.isLight) Color.parseColor("#6B94E8") else Color.parseColor("#76A1ED")
        val progress = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = 12f
        }
        val clip = ClipDrawable(progress, Gravity.START, ClipDrawable.HORIZONTAL)
        return LayerDrawable(arrayOf(bg, clip)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }
    private fun createCvdDrawable(): LayerDrawable {
        val bg = GradientDrawable().apply {
            setColor(AppTheme.redDeep())
            cornerRadius = 12f
        }
        val fg = GradientDrawable().apply {
            setColor(AppTheme.greenDeep())
            cornerRadius = 12f
        }
        val clip = ClipDrawable(fg, Gravity.START, ClipDrawable.HORIZONTAL)
        return LayerDrawable(arrayOf(bg, clip)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }
}
