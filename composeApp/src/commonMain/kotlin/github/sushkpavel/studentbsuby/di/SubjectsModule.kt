package github.sushkpavel.studentbsuby.di

import github.sushkpavel.studentbsuby.data.models.Subject
import github.sushkpavel.studentbsuby.repo.CurrentSemesterRepository
import github.sushkpavel.studentbsuby.repo.SubjectsRepository
import github.sushkpavel.studentbsuby.ui.screens.drawer.subjects.SubjectsEventHandlerImpl
import github.sushkpavel.studentbsuby.ui.screens.drawer.subjects.SubjectsViewModel
import github.sushkpavel.studentbsuby.util.DataState
import github.sushkpavel.studentbsuby.util.communication.StateFlowCommunication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subjectsModule = module {

    single { SubjectsRepository(get(), get(), get()) }

    factory { CurrentSemesterRepository(get(), get(), get()) }

    viewModel {
        val isUpdating = StateFlowCommunication(false)
        val semester = StateFlowCommunication(0)
        val search = StateFlowCommunication("")
        val withExam = StateFlowCommunication(false)
        val withCredit = StateFlowCommunication(false)
        val subjects = StateFlowCommunication<DataState<List<List<Subject>>>>(DataState.Loading)
        val visibleSubjects = StateFlowCommunication<DataState<List<List<Subject>>>>(DataState.Loading)

        val eventHandler = SubjectsEventHandlerImpl(
            subjectRepository = get(),
            currentSemesterRepository = get(),
            connectivityManager = get(),
            isUpdatingMapper = isUpdating,
            semesterMapper = semester,
            searchMapper = search,
            withExamMapper = withExam,
            withCreditMapper = withCredit,
            subjectsMapper = subjects,
            visibleSubjectsMapper = visibleSubjects
        )

        SubjectsViewModel(
            isUpdating = isUpdating,
            semesterCommunication = semester,
            searchCommunication = search,
            withExamCommunication = withExam,
            withCreditCommunication = withCredit,
            subjectsCommunication = subjects,
            visibleSubjectsCommunication = visibleSubjects,
            eventHandler = eventHandler,
            errorHandler = get(),
            dispatchers = get()
        )
    }
}
