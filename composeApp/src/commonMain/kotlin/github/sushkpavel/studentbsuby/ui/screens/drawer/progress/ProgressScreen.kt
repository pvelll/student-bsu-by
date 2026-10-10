package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.empty
import github.sushkpavel.studentbsuby.resources.progress
import github.sushkpavel.studentbsuby.resources.progress_empty
import github.sushkpavel.studentbsuby.resources.progress_semester_filter
import github.sushkpavel.studentbsuby.resources.something_gone_wrong
import github.sushkpavel.studentbsuby.ui.common.BsuProgressBar
import github.sushkpavel.studentbsuby.ui.common.BsuProgressBarSwipeRefreshIndicator
import github.sushkpavel.studentbsuby.ui.common.ErrorScreen
import github.sushkpavel.studentbsuby.ui.common.NavigationMenuButton
import github.sushkpavel.studentbsuby.ui.common.swiperefresh.SwipeRefresh
import github.sushkpavel.studentbsuby.ui.common.swiperefresh.rememberSwipeRefreshState
import github.sushkpavel.studentbsuby.ui.common.toolbar.CollapsingToolbarScaffold
import github.sushkpavel.studentbsuby.ui.common.toolbar.ScrollStrategy
import github.sushkpavel.studentbsuby.ui.common.toolbar.rememberCollapsingToolbarScaffoldState
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.bsuBackgroundPattern
import github.sushkpavel.studentbsuby.util.communication.collectAsState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    isTablet: Boolean,
    onMenuClicked: () -> Unit,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.progressCommunication.collectAsState()

    when (val value = state) {
        is DataState.Success -> SuccessProgressScreen(
            isTablet = isTablet,
            progress = value.value,
            viewModel = viewModel,
            onMenuClicked = onMenuClicked
        )
        is DataState.Loading -> LoadingProgressScreen(
            isTablet = isTablet,
            onMenuClicked = onMenuClicked
        )
        is DataState.Empty -> ErrorScreen(
            isTablet = isTablet,
            toolbarText = stringResource(Res.string.progress),
            title = stringResource(Res.string.empty),
            error = stringResource(Res.string.progress_empty),
            updater = viewModel,
            onMenuClicked = onMenuClicked
        )
        is DataState.Error -> ErrorScreen(
            isTablet = isTablet,
            toolbarText = stringResource(Res.string.progress),
            title = stringResource(Res.string.something_gone_wrong),
            error = stringResource(value.message),
            updater = viewModel,
            onMenuClicked = onMenuClicked
        )
    }
}

@Composable
private fun ProgressToolbar(
    isTablet: Boolean,
    onMenuClicked: () -> Unit,
) {
    TopAppBar(
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        if (!isTablet) {
            NavigationMenuButton(onClick = onMenuClicked)
        }
        Text(
            text = stringResource(Res.string.progress),
            color = MaterialTheme.colors.onSecondary,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@Composable
private fun LoadingProgressScreen(
    isTablet: Boolean,
    onMenuClicked: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .bsuBackgroundPattern(MaterialTheme.colors.primary.copy(alpha = .05f), true),
    ) {
        Column(
            Modifier
                .background(MaterialTheme.colors.secondary)
                .zIndex(2f)
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            ProgressToolbar(isTablet = isTablet, onMenuClicked = onMenuClicked)
        }
        BsuProgressBar(
            modifier = Modifier.align(Alignment.Center),
            size = 100.dp,
            tint = MaterialTheme.colors.primary
        )
    }
}

@Composable
private fun SuccessProgressScreen(
    isTablet: Boolean,
    progress: AcademicProgress,
    viewModel: ProgressViewModel,
    onMenuClicked: () -> Unit,
) {
    val scaffoldState = rememberCollapsingToolbarScaffoldState()
    val isRefreshing by viewModel.isUpdating.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    var semesterFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    val filter = semesterFilter?.takeIf { it in progress.ratedSemesters }

    Column {
        Spacer(
            modifier = Modifier
                .zIndex(2f)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colors.secondary)
        )
        CollapsingToolbarScaffold(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            state = scaffoldState,
            scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
            toolbarModifier = Modifier.background(MaterialTheme.colors.secondary),
            toolbar = {
                ProgressToolbar(isTablet = isTablet, onMenuClicked = onMenuClicked)
            }
        ) {
            SwipeRefresh(
                state = refreshState,
                onRefresh = viewModel::update,
                indicator = { state, offset ->
                    BsuProgressBarSwipeRefreshIndicator(state = state, trigger = offset)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .bsuBackgroundPattern(MaterialTheme.colors.primary.copy(alpha = .05f), true)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = refreshState.indicatorOffset },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    progressItems(
                        progress = progress,
                        semesterFilter = filter,
                        onSemesterFilterChanged = { semesterFilter = it }
                    )
                    item {
                        Spacer(
                            modifier = Modifier.windowInsetsBottomHeight(
                                WindowInsets.navigationBars.add(WindowInsets(bottom = 10.dp))
                            )
                        )
                    }
                }
            }
        }
    }
}

private val ItemModifier = Modifier
    .widthIn(max = ProgressContentWidth)
    .fillMaxWidth()
    .padding(horizontal = 10.dp, vertical = 6.dp)

private fun LazyListScope.progressItems(
    progress: AcademicProgress,
    semesterFilter: Int?,
    onSemesterFilterChanged: (Int?) -> Unit,
) {
    item(key = "hero") {
        AverageHeroCard(progress = progress, modifier = ItemModifier)
    }
    item(key = "chart") {
        ChartCard(progress = progress, modifier = ItemModifier)
    }
    if (progress.hasMarks) {
        item(key = "summary") {
            SummaryCard(progress = progress, modifier = ItemModifier)
        }
    }
    progress.session?.let { session ->
        item(key = "session") {
            SessionCard(session = session, modifier = ItemModifier)
        }
    }
    if (progress.hasMarks) {
        if (progress.ratedSemesters.size > 1) {
            item(key = "filter") {
                Column(ItemModifier) {
                    Text(
                        text = stringResource(Res.string.progress_semester_filter),
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(start = 5.dp, bottom = 8.dp, top = 4.dp)
                    )
                    SemesterChips(
                        semesters = progress.ratedSemesters,
                        selected = semesterFilter,
                        onSelected = onSemesterFilterChanged
                    )
                }
            }
        }
        item(key = "rating") {
            RatingCard(progress = progress, semester = semesterFilter, modifier = ItemModifier)
        }
        item(key = "distribution") {
            DistributionCard(progress = progress, semester = semesterFilter, modifier = ItemModifier)
        }
    }
    item(key = "goal") {
        GoalCalculatorCard(progress = progress, modifier = ItemModifier)
    }
}
