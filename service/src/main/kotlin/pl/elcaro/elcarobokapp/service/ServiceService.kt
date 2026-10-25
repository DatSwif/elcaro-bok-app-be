package pl.elcaro.elcarobokapp.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.elcaro.elcarobokapp.domain.services.ClientPackageSubscription
import pl.elcaro.elcarobokapp.infrastructure.*
import java.util.*

@Service
@Transactional(readOnly = true)
class ServiceService(
    private val serviceRepository: ServiceRepository,
    private val clientServiceRepository: ClientServiceRepository,
    private val clientAddressRepository: ClientAddressRepository,
    private val tvChannelPackageRepository: TVChannelPackageRepository,
    private val clientPackageSubscriptionRepository: ClientPackageSubscriptionRepository,
    private val clientHardwareRepository: ClientHardwareRepository,
) {
    fun getServicesByClientNumber(clientNumber: String, locale: Locale): List<ServiceListView> =
        clientServiceRepository.findAllByClientNumber(clientNumber)
            .map { it.toListView(locale) }

    fun getServiceById(id: Long, clientNumber: String, locale: Locale): ServiceDetailView {
        val clientService = clientServiceRepository.findByIdAndClientNumber(id, clientNumber)
            .orElseThrow { NoSuchElementException("Service with id $id not found") }

        val serviceId = clientService.service.id!!
        val allPackages = tvChannelPackageRepository.findAllByServiceId(serviceId)
        val subscribedIds = clientPackageSubscriptionRepository
            .findAllByClientNumberAndServiceId(clientNumber, serviceId)
            .map { it.tvChannelPackageId }
            .toSet()

        val subscribedPackages = allPackages.filter { it.id in subscribedIds || it.isBasePackage }
            .map { it.toView(locale) }
        val availableAddOns = allPackages.filter { it.id !in subscribedIds && !it.isBasePackage }
            .map { it.toView(locale) }

        return clientService.toDetailView(
            locale = locale,
            packages = subscribedPackages,
            availableAddOns = availableAddOns.ifEmpty { null },
        )
    }

    fun getAddressesByClientNumber(clientNumber: String): List<AddressView> =
        clientAddressRepository.findAllByClientClientNumber(clientNumber)
            .map { it.toView() }

    fun getAllPackagesByService(serviceId: Long, clientNumber: String, locale: Locale): List<TVChannelPackageView> {
        val clientService = clientServiceRepository.findByIdAndClientNumber(serviceId, clientNumber)
            .orElseThrow { NoSuchElementException("Service with id $serviceId not found") }
        val packages = tvChannelPackageRepository.findAllByServiceId(clientService.service.id!!)
        if (packages.isEmpty()) throw NoSuchElementException("No packages available for service with id $serviceId")
        return packages.map { it.toView(locale) }
    }

    fun getChannelPackageById(packageId: Long, serviceId: Long, clientNumber: String, locale: Locale): TVChannelPackageView =
        tvChannelPackageRepository.findByIdAndServiceIdAndClientNumber(packageId, serviceId, clientNumber)
            .orElseThrow { NoSuchElementException("Package with id $packageId not found") }
            .toView(locale)

    fun getAllHardwareByClientNumber(clientNumber: String, locale: Locale): List<HardwareItemView> =
        clientHardwareRepository.findAllByClientNumber(clientNumber).map { it.toItemView(locale) }

    fun getHardwareById(id: Long, clientNumber: String, locale: Locale): HardwareItemView =
        clientHardwareRepository.findByIdAndClientNumber(id, clientNumber)
            .orElseThrow { NoSuchElementException("Hardware with id $id not found") }
            .toItemView(locale)

    @Transactional
    fun subscribeToPackage(clientNumber: String, serviceId: Long, packageId: Long) {
        if (!clientServiceRepository.existsByClientClientNumberAndServiceId(clientNumber, serviceId)) {
            throw NoSuchElementException("Service with id $serviceId not found")
        }
        val pkg = tvChannelPackageRepository.findById(packageId)
            .orElseThrow { NoSuchElementException("Package with id $packageId not found") }
        if (pkg.serviceId != serviceId) {
            throw NoSuchElementException("Package with id $packageId not found")
        }
        if (!clientPackageSubscriptionRepository.existsByClientNumberAndTvChannelPackageId(clientNumber, packageId)) {
            clientPackageSubscriptionRepository.save(ClientPackageSubscription(clientNumber, packageId, serviceId))
        }
    }

    @Transactional
    fun unsubscribeFromPackage(clientNumber: String, serviceId: Long, packageId: Long) {
        if (!clientServiceRepository.existsByClientClientNumberAndServiceId(clientNumber, serviceId)) {
            throw NoSuchElementException("Service with id $serviceId not found")
        }
        val pkg = tvChannelPackageRepository.findById(packageId)
            .orElseThrow { NoSuchElementException("Package with id $packageId not found") }
        if (pkg.serviceId != serviceId) {
            throw NoSuchElementException("Package with id $packageId not found")
        }
        clientPackageSubscriptionRepository.deleteByClientNumberAndTvChannelPackageId(clientNumber, packageId)
    }
}
