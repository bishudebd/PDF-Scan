package com.example.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.data.model.ScanMode
import com.example.ui.screens.AboutAppScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CropEditScreen
import com.example.ui.screens.EnhanceScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MyDocumentsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import com.example.ui.viewmodel.ActivePdfTool
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PdfPreviewViewModel
import com.example.ui.viewmodel.PdfToolsViewModel
import com.example.ui.viewmodel.ScannerEditorViewModel
import com.example.ui.viewmodel.SettingsViewModel

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val FOLDERS = "folders"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val SETTINGS_APP = "settings_app"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_BACKUP_RESTORE = "settings_backup_restore"
    const val SETTINGS_ABOUT = "settings_about"
    const val SETTINGS_PRIVACY_POLICY = "settings_privacy_policy"
    const val CAMERA_SCANNER = "camera_scanner"
    const val CROP_EDIT = "crop_edit"
    const val ENHANCE = "enhance"
    const val PDF_PREVIEW = "pdf_preview/{documentId}"

    fun pdfPreview(documentId: Long) = "pdf_preview/$documentId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun PdfScanNavHost(
    container: AppContainer,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val scannerViewModel: ScannerEditorViewModel = viewModel(factory = ScannerEditorViewModel.factory(container))
    val toolsViewModel: PdfToolsViewModel = viewModel(factory = PdfToolsViewModel.factory(container))

    val userSettings by mainViewModel.userSettings.collectAsStateWithLifecycle()
    val recentDocs by mainViewModel.recentDocuments.collectAsStateWithLifecycle()
    val allDocs by mainViewModel.allDocuments.collectAsStateWithLifecycle()
    val allFolders by mainViewModel.allFolders.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            mainViewModel.clearStatusMessage()
        }
    }

    // Zero-permission Android Photo Picker for Import Images & Image to PDF
    val importImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scannerViewModel.importImageUris(
                uris = uris,
                appendToExisting = false,
                titlePrefix = "ImagePDF"
            ) {
                navController.navigate(Routes.CROP_EDIT)
            }
        }
    }

    fun startCameraScan(mode: ScanMode = ScanMode.DOCUMENT) {
        scannerViewModel.startFreshSession(mode = mode)
        navController.navigate(Routes.CAMERA_SCANNER)
    }

    fun launchPhotoPicker() {
        importImagesLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun openTool(tool: ActivePdfTool, docId: Long? = null) {
        toolsViewModel.selectTool(tool, docId)
        navController.navigate(Routes.TOOLS) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomTabs = remember {
        listOf(
            BottomNavTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavTab(Routes.DOCUMENTS, "Documents", Icons.Filled.Description, Icons.Outlined.Description),
            BottomNavTab(Routes.FOLDERS, "Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
            BottomNavTab(Routes.TOOLS, "PDF Tools", Icons.Filled.Build, Icons.Outlined.Build)
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (tab.route == Routes.DOCUMENTS) {
                                    mainViewModel.selectFolder(null)
                                }
                                if (tab.route == Routes.TOOLS && currentRoute == Routes.TOOLS) {
                                    toolsViewModel.selectTool(ActivePdfTool.HUB)
                                }
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CobaltBlue,
                                selectedTextColor = CobaltBlue,
                                indicatorColor = CobaltBlueSoft,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.HOME || currentRoute == Routes.DOCUMENTS) {
                FloatingActionButton(
                    onClick = { startCameraScan(ScanMode.DOCUMENT) },
                    containerColor = CobaltBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_scan_document")
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = "Scan Document")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onboardingCompleted = userSettings?.onboardingCompleted,
                        onFinished = { completed ->
                            val target = if (completed) Routes.HOME else Routes.ONBOARDING
                            navController.navigate(target) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onComplete = {
                            mainViewModel.completeOnboarding()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeDashboardScreen(
                        recentDocuments = recentDocs,
                        totalDocCount = allDocs.size,
                        folders = allFolders,
                        onScanDocument = { startCameraScan(ScanMode.DOCUMENT) },
                        onImportImages = { launchPhotoPicker() },
                        onImageToPdf = { launchPhotoPicker() },
                        onPdfToImages = { openTool(ActivePdfTool.PDF_TO_IMAGES) },
                        onMergePdf = { openTool(ActivePdfTool.MERGE) },
                        onSplitPdf = { openTool(ActivePdfTool.SPLIT) },
                        onCompressPdf = { openTool(ActivePdfTool.COMPRESS) },
                        onSignPdf = { openTool(ActivePdfTool.SIGN) },
                        onOcrScanner = { openTool(ActivePdfTool.OCR) },
                        onIdCardScanner = { startCameraScan(ScanMode.ID_CARD) },
                        onOpenSearch = {
                            mainViewModel.selectFolder(null)
                            navController.navigate(Routes.DOCUMENTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onViewAllDocuments = {
                            mainViewModel.selectFolder(null)
                            navController.navigate(Routes.DOCUMENTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenDocument = { docId -> navController.navigate(Routes.pdfPreview(docId)) },
                        onRenameDocument = { id, title -> mainViewModel.renameDocument(id, title) },
                        onMoveDocument = { id, folderId -> mainViewModel.moveDocumentToFolder(id, folderId) },
                        onDeleteDocument = { id -> mainViewModel.deleteDocument(id) }
                    )
                }

                composable(Routes.DOCUMENTS) {
                    MyDocumentsScreen(
                        viewModel = mainViewModel,
                        onScanNew = { startCameraScan(ScanMode.DOCUMENT) },
                        onOpenDocument = { docId -> navController.navigate(Routes.pdfPreview(docId)) }
                    )
                }

                composable(Routes.FOLDERS) {
                    FoldersScreen(
                        viewModel = mainViewModel,
                        onOpenFolderDocuments = { folderId ->
                            mainViewModel.selectFolder(folderId)
                            navController.navigate(Routes.DOCUMENTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Routes.TOOLS) {
                    PdfToolsScreen(
                        viewModel = toolsViewModel,
                        onImageToPdf = { launchPhotoPicker() },
                        onIdCardScanner = { startCameraScan(ScanMode.ID_CARD) },
                        onCameraOcr = { startCameraScan(ScanMode.OCR) },
                        onOpenDocument = { docId -> navController.navigate(Routes.pdfPreview(docId)) }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAppSettings = { navController.navigate(Routes.SETTINGS_APP) },
                        onOpenAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                        onOpenBackupRestore = { navController.navigate(Routes.SETTINGS_BACKUP_RESTORE) },
                        onOpenAboutApp = { navController.navigate(Routes.SETTINGS_ABOUT) },
                        onOpenPrivacyPolicy = { navController.navigate(Routes.SETTINGS_PRIVACY_POLICY) }
                    )
                }

                composable(Routes.SETTINGS_APP) {
                    AppSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_APPEARANCE) {
                    AppearanceSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_BACKUP_RESTORE) {
                    BackupRestoreScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_ABOUT) {
                    AboutAppScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_PRIVACY_POLICY) {
                    PrivacyPolicyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.CAMERA_SCANNER) {
                    CameraScannerScreen(
                        viewModel = scannerViewModel,
                        onClose = { navController.popBackStack() },
                        onProceedToCrop = { navController.navigate(Routes.CROP_EDIT) },
                        onProceedToEnhance = { navController.navigate(Routes.ENHANCE) },
                        onOcrImageCaptured = { path ->
                            toolsViewModel.selectTool(ActivePdfTool.OCR)
                            toolsViewModel.runOcrOnLocalFile(path)
                            navController.navigate(Routes.TOOLS) {
                                popUpTo(Routes.CAMERA_SCANNER) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.CROP_EDIT) {
                    CropEditScreen(
                        viewModel = scannerViewModel,
                        onBack = { navController.popBackStack() },
                        onAddMorePages = {
                            scannerViewModel.clearRescanMode()
                            navController.navigate(Routes.CAMERA_SCANNER)
                        },
                        onRescanActivePage = {
                            navController.navigate(Routes.CAMERA_SCANNER)
                        },
                        onProceedToEnhance = { navController.navigate(Routes.ENHANCE) }
                    )
                }

                composable(Routes.ENHANCE) {
                    EnhanceScreen(
                        viewModel = scannerViewModel,
                        onBack = { navController.popBackStack() },
                        onAddPage = {
                            scannerViewModel.clearRescanMode()
                            navController.navigate(Routes.CAMERA_SCANNER)
                        },
                        onRescanPage = {
                            navController.navigate(Routes.CAMERA_SCANNER)
                        },
                        onCropPage = {
                            navController.navigate(Routes.CROP_EDIT)
                        },
                        onSavedDocument = { docId ->
                            mainViewModel.postMessage("PDF saved to My Documents")
                            navController.navigate(Routes.pdfPreview(docId)) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        }
                    )
                }

                composable(
                    route = Routes.PDF_PREVIEW,
                    arguments = listOf(navArgument("documentId") { type = NavType.LongType })
                ) { entry ->
                    val docId = entry.arguments?.getLong("documentId") ?: return@composable
                    val previewViewModel: PdfPreviewViewModel = viewModel(
                        key = "preview_$docId",
                        factory = PdfPreviewViewModel.factory(docId, container)
                    )
                    PdfPreviewScreen(
                        viewModel = previewViewModel,
                        onBack = { navController.popBackStack() },
                        onEditDocument = { id ->
                            scannerViewModel.loadExistingDocumentForEditing(id) {
                                navController.navigate(Routes.ENHANCE)
                            }
                        },
                        onSignDocument = { id ->
                            openTool(ActivePdfTool.SIGN, id)
                        }
                    )
                }
            }
        }
    }
}
