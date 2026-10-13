package github.sushkpavel.studentbsuby.ui.screens.drawer.progress

import github.sushkpavel.studentbsuby.data.models.Subject
import github.sushkpavel.studentbsuby.repo.CurrentSemesterRepository
import github.sushkpavel.studentbsuby.repo.DataSource
import github.sushkpavel.studentbsuby.repo.SubjectsRepository
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.error_load_subjects
import github.sushkpavel.studentbsuby.util.BaseSuspendEventHandler
import github.sushkpavel.studentbsuby.util.ConnectivityManager
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.SuspendEventHandler
import github.sushkpavel.studentbsuby.util.communication.Mapper
import github.sushkpavel.studentbsuby.util.communication.StateMapper
import github.sushkpavel.studentbsuby.util.onReconnected
import github.sushkpavel.studentbsuby.util.toErrorMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ProgressEventHandler : SuspendEventHandler<ProgressEvent>

class ProgressEventHandlerImpl(
    subjectsRepository: SubjectsRepository,
    currentSemesterRepository: CurrentSemesterRepository,
    connectivityManager: ConnectivityManager,
    isUpdatingMapper: Mapper<Boolean>,
    progressMapper: StateMapper<DataState<AcademicProgress>>,
) : ProgressEventHandler, SuspendEventHandler<ProgressEvent> by SuspendEventHandler.from(
    UpdateRequestedHandler(
        subjectsRepository = subjectsRepository,
        currentSemesterRepository = currentSemesterRepository,
        connectivityManager = connectivityManager,
        isUpdatingMapper = isUpdatingMapper,
        progressMapper = progressMapper
    )
)

private class UpdateRequestedHandler(
    private val subjectsRepository: SubjectsRepository,
    private val currentSemesterRepository: CurrentSemesterRepository,
    private val connectivityManager: ConnectivityManager,
    private val isUpdatingMapper: Mapper<Boolean>,
    private val progressMapper: StateMapper<DataState<AcademicProgress>>,
) : BaseSuspendEventHandler<ProgressEvent.UpdateRequested>(
    ProgressEvent.UpdateRequested::class
) {

    private val mutex = Mutex()
    private var subjects: List<List<Subject>>? = null
    private var currentSemester: Int? = null

    override suspend fun launch() {
        isUpdatingMapper.map(false)
        progressMapper.map(DataState.Loading)
        update(DataSource.All)
        connectivityManager.onReconnected {
            update(DataSource.Remote)
        }
    }

    override suspend fun handle(event: ProgressEvent.UpdateRequested) {
        isUpdatingMapper.map(true)
        update(DataSource.Remote)
        isUpdatingMapper.map(false)
    }

    private suspend fun update(dataSource: DataSource) = coroutineScope {
        launch {
            currentSemesterRepository.get(dataSource)
                .onEach {
                    mutex.withLock {
                        currentSemester = it
                        publish()
                    }
                }
                .catch { }
                .collect()
        }

        subjectsRepository.get(dataSource)
            .onEach {
                mutex.withLock {
                    subjects = it
                    publish()
                }
            }
            .onEmpty {
                if (progressMapper.current !is DataState.Success)
                    progressMapper.map(DataState.Empty)
            }
            .catch {
                if (progressMapper.current !is DataState.Success) {
                    progressMapper.map(
                        DataState.Error(it.toErrorMessage(Res.string.error_load_subjects), it)
                    )
                }
            }.collect()
    }

    private fun publish() {
        val subjects = subjects ?: return
        progressMapper.map(
            if (subjects.all { it.isEmpty() }) DataState.Empty
            else DataState.Success(AcademicProgress.from(subjects, currentSemester))
        )
    }
}
