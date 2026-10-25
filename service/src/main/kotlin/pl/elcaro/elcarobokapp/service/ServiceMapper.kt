package pl.elcaro.elcarobokapp.service

import pl.elcaro.elcarobokapp.domain.services.*
import pl.elcaro.elcarobokapp.domain.services.ClientService
import java.util.*

fun ClientService.toListView(locale: Locale): ServiceListView {
    val translation = service.findTranslation(locale)
    return ServiceListView(
        id = id,
        addressId = clientAddress?.id,
        name = translation?.name ?: "",
        status = status,
        monthlyFee = service.monthlyFee,
        serviceType = serviceType,
    )
}

fun ClientService.toDetailView(
    locale: Locale,
    packages: List<TVChannelPackageView> = emptyList(),
    availableAddOns: List<TVChannelPackageView>? = null,
): ServiceDetailView {
    val translation = service.findTranslation(locale)
    return ServiceDetailView(
        userServiceId = id,
        addressId = clientAddress?.id,
        name = translation?.name ?: "",
        status = status,
        monthlyFee = service.monthlyFee,
        serviceType = serviceType,
        startDate = startDate,
        endDate = endDate,
        description = translation?.info,
        relatedHardware = service.hardware.map { it.toView(locale) },
        packages = packages,
        availableAddOns = availableAddOns,
    )
}

fun ClientAddress.toView(): AddressView = AddressView(
    addressId = id,
    address = address,
    contractActiveFrom = contractActiveFrom,
    contractActiveUntil = contractActiveUntil,
)

fun Hardware.toView(locale: Locale): HardwareView {
    val translation = findTranslation(locale)
    return HardwareView(
        id = id,
        name = translation?.name ?: "",
        imagePath = imagePath,
    )
}

fun TVChannelPackage.toView(locale: Locale): TVChannelPackageView {
    val translation = translations.firstOrNull { it.id.languageCode == locale.language }
        ?: translations.firstOrNull { it.id.languageCode == "pl" }
        ?: translations.firstOrNull()
    return TVChannelPackageView(
        id = id,
        name = translation?.name ?: name,
        monthlyFee = monthlyFee,
        isBasePackage = isBasePackage,
        channels = channels.map { it.toView(locale) },
    )
}

fun TVChannel.toView(locale: Locale): TVChannelView {
    val translation = translations.firstOrNull { it.id.languageCode == locale.language }
        ?: translations.firstOrNull { it.id.languageCode == "pl" }
        ?: translations.firstOrNull()
    return TVChannelView(
        name = translation?.name ?: "",
        pictogram = Base64.getEncoder().encodeToString(pictogram),
    )
}

private fun Service.findTranslation(locale: Locale): ServiceTranslation? =
    translations.firstOrNull { it.id.languageCode == locale.language }
        ?: translations.firstOrNull { it.id.languageCode == "pl" }
        ?: translations.firstOrNull()

fun ClientHardware.toItemView(locale: Locale): HardwareItemView {
    val translation = hardware.translations.firstOrNull { it.id.languageCode == locale.language }
        ?: hardware.translations.firstOrNull { it.id.languageCode == "pl" }
        ?: hardware.translations.firstOrNull()
    return HardwareItemView(
        id = id,
        name = translation?.name ?: "",
        serviceType = clientService.serviceType,
        owner = ownership,
        icon = hardware.imagePath,
        serialNumber = serialNumber,
        isReturnable = hardware.isReturnable,
        isReportable = hardware.isReportable,
    )
}

private fun Hardware.findTranslation(locale: Locale): HardwareTranslation? =
    translations.firstOrNull { it.id.languageCode == locale.language }
        ?: translations.firstOrNull { it.id.languageCode == "pl" }
        ?: translations.firstOrNull()
