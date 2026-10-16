package github.sushkpavel.studentbsuby.repo

import github.sushkpavel.studentbsuby.api.PaidServicesApi
import github.sushkpavel.studentbsuby.dao.PaidServicesDao
import github.sushkpavel.studentbsuby.data.models.*
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.hostel_payment
import github.sushkpavel.studentbsuby.util.ResourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaidServicesRepository(
    usernameProvider: UsernameProvider,
    api : PaidServicesApi,
    dao : PaidServicesDao,
    resources : ResourceManager
) {

    private val infoAndBillsRepository: Repository<Pair<PaidServicesInfo, List<Bill>>> =
        InfoAndBillsRepository(usernameProvider, api, dao)

    private val tuitionFeeRepository: Repository<List<TuitionFeePayment>> =
        TuitionFeeReceiptsRepository(usernameProvider, api, dao)

    private val academDebtRepository : Repository<List<Receipt>> =
        AcademDebtRepository(usernameProvider,api,dao)

    private val hostelBillsRepository : Repository<List<Bill>> =
        HostelBillsRepository( usernameProvider ,api, dao, resources)

    private val commonReceiptsRepository : Repository<List<Receipt>> =
        CommonReceiptsRepository(usernameProvider, api, dao)

    val eripUrl get() = "https://student.bsu.by/PersonalCabinet/Pay/docs/%D0%98%D0%BD%D1%81%D1%82%D1%80%D1%83%D0%BA%D1%86%D0%B8%D1%8F%20%D0%BE%20%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B8%20%D0%BE%D0%BF%D0%BB%D0%B0%D1%82%D1%8B%20%D1%87%D0%B5%D1%80%D0%B5%D0%B7%20%D0%90%D0%98%D0%A1%20%D0%95%D0%A0%D0%98%D0%9F.pdf"

    fun getInfoAndBills(source : DataSource = DataSource.All): Flow<Pair<PaidServicesInfo, List<Bill>>> =
        infoAndBillsRepository.get(source)

    fun getTuitionFeeReceipts(source : DataSource = DataSource.All): Flow<List<TuitionFeePayment>> =
        tuitionFeeRepository.get(source).map { it.sortedByDescending(TuitionFeePayment::deadline) }

    fun getAcademDebtFeeReceipts(source : DataSource = DataSource.All): Flow<List<Receipt>> =
        academDebtRepository.get(source).map { it.sortedByDescending(Receipt::deadline) }

    fun getHostelBills(source : DataSource = DataSource.All): Flow<List<Bill>> =
        hostelBillsRepository.get(source).map { it.sortedByDescending(Bill::deadline) }

    fun getCommonReceipts(source : DataSource = DataSource.All): Flow<List<Receipt>> =
        commonReceiptsRepository.get(source).map { it.sortedByDescending(Receipt::deadline) }}

private class CommonReceiptsRepository(
    private val usernameProvider: UsernameProvider,
    private val api: PaidServicesApi,
    private val dao: PaidServicesDao,
) : CacheWebRepository<List<Receipt>>() {

    override suspend fun getFromCache(): List<Receipt>? = kotlin.runCatching{
        dao.getReceipts(usernameProvider.username, Receipt.TYPE_COMMON)
    }.getOrNull()

    override suspend fun getFromWeb(): List<Receipt>? =
        PaidServicesParser.parseReceipts(api.common().html(), usernameProvider.username, Receipt.TYPE_COMMON)

    override suspend fun saveToCache(value: List<Receipt>) {
        kotlin.runCatching {
            dao.clearReceipts(usernameProvider.username, Receipt.TYPE_COMMON)
            value.forEach{dao.insertReceipt(it)}
        }
    }

}
private class HostelBillsRepository(
    private val usernameProvider: UsernameProvider,
    private val api: PaidServicesApi,
    private val dao: PaidServicesDao,
    private val resources: ResourceManager
) : CacheWebRepository<List<Bill>>() {

    override suspend fun getFromCache(): List<Bill>? = kotlin.runCatching {
        dao.getBills(usernameProvider.username, Bill.TYPE_HOSTEL)
    }.getOrNull()

    override suspend fun getFromWeb(): List<Bill>? =
        PaidServicesParser.parseHostelBills(
            html = api.hostelBills().html(),
            owner = usernameProvider.username,
            paymentType = resources.getString(Res.string.hostel_payment)
        )

    override suspend fun saveToCache(value: List<Bill>) {
        kotlin.runCatching {
            dao.clearBills(usernameProvider.username, Bill.TYPE_HOSTEL)
            value.forEach { dao.insertBill(it) }
        }
    }
}

private class AcademDebtRepository(
    private val usernameProvider: UsernameProvider,
    private val api : PaidServicesApi,
    private val dao : PaidServicesDao,
) : CacheWebRepository<List<Receipt>>() {

    override suspend fun getFromCache(): List<Receipt>? = kotlin.runCatching {
        dao.getReceipts(usernameProvider.username, Receipt.TYPE_ACADEM_DEBT)
    }.getOrNull()

    override suspend fun getFromWeb(): List<Receipt>? =
        PaidServicesParser.parseReceipts(api.academicDebt().html(), usernameProvider.username, Receipt.TYPE_ACADEM_DEBT)

    override suspend fun saveToCache(value: List<Receipt>) {
        kotlin.runCatching {
            dao.clearReceipts(usernameProvider.username, Receipt.TYPE_ACADEM_DEBT)
            value.forEach { dao.insertReceipt(it) }
        }
    }
}

private class InfoAndBillsRepository(
    private val usernameProvider: UsernameProvider,
    private val api : PaidServicesApi,
    private val dao : PaidServicesDao,
) : CacheWebRepository<Pair<PaidServicesInfo, List<Bill>>>() {

    override suspend fun getFromCache(): Pair<PaidServicesInfo, List<Bill>>? = kotlin.runCatching {
        dao.getInfo(usernameProvider.username)?.let {
            it to dao.getBills(usernameProvider.username, Bill.TYPE_PAIDINFO)
        }
    }.getOrNull()

    override suspend fun getFromWeb(): Pair<PaidServicesInfo, List<Bill>>? =
        PaidServicesParser.parseInfo(api.info().html(), usernameProvider.username)

    override suspend fun saveToCache(value: Pair<PaidServicesInfo, List<Bill>>) {
        kotlin.runCatching {
            dao.insertInfo(value.first)
            dao.clearBills(value.first.owner, Bill.TYPE_PAIDINFO)
            value.second.forEach { dao.insertBill(it) }
        }
    }
}

private class TuitionFeeReceiptsRepository(
    private val usernameProvider: UsernameProvider,
    private val api : PaidServicesApi,
    private val dao : PaidServicesDao,
) : CacheWebRepository<List<TuitionFeePayment>>() {

    override suspend fun getFromCache(): List<TuitionFeePayment>? = kotlin.runCatching {
        dao.getTuitionFeeReceipts(usernameProvider.username)
    }.getOrNull()

    override suspend fun getFromWeb(): List<TuitionFeePayment>? =
        PaidServicesParser.parseTuitionFees(api.tuitionFee().html(), usernameProvider.username)

    override suspend fun saveToCache(value: List<TuitionFeePayment>) {
        kotlin.runCatching {
            dao.clearTuitionFeeReceipts(usernameProvider.username)
            value.forEach { dao.insertTuitionFeeReceipt(it) }
        }
    }
}
