package pl.elcaro.elcarobokapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import pl.elcaro.elcarobokapp.security.TestSecurityConfigurationFull

@SpringBootApplication
@Import(TestSecurityConfigurationFull::class)
class TestApplication
