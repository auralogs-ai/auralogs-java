module ai.auralogs.slf4j {
    requires ai.auralogs.core;
    requires org.slf4j;
    requires static org.jspecify;

    exports ai.auralogs.slf4j;

    provides org.slf4j.spi.SLF4JServiceProvider
        with ai.auralogs.slf4j.AuralogsSlf4jServiceProvider;
}
