package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final CamundaLibraryAccessors laccForCamundaLibraryAccessors = new CamundaLibraryAccessors(owner);
    private final JunitLibraryAccessors laccForJunitLibraryAccessors = new JunitLibraryAccessors(owner);
    private final MockitoLibraryAccessors laccForMockitoLibraryAccessors = new MockitoLibraryAccessors(owner);
    private final Resilience4jLibraryAccessors laccForResilience4jLibraryAccessors = new Resilience4jLibraryAccessors(owner);
    private final SpringLibraryAccessors laccForSpringLibraryAccessors = new SpringLibraryAccessors(owner);
    private final SpringdocLibraryAccessors laccForSpringdocLibraryAccessors = new SpringdocLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>h2</b> with <b>com.h2database:h2</b> coordinates and
     * with version reference <b>h2</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getH2() {
        return create("h2");
    }

    /**
     * Dependency provider for <b>lombok</b> with <b>org.projectlombok:lombok</b> coordinates and
     * with version reference <b>lombok</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getLombok() {
        return create("lombok");
    }

    /**
     * Group of libraries at <b>camunda</b>
     */
    public CamundaLibraryAccessors getCamunda() {
        return laccForCamundaLibraryAccessors;
    }

    /**
     * Group of libraries at <b>junit</b>
     */
    public JunitLibraryAccessors getJunit() {
        return laccForJunitLibraryAccessors;
    }

    /**
     * Group of libraries at <b>mockito</b>
     */
    public MockitoLibraryAccessors getMockito() {
        return laccForMockitoLibraryAccessors;
    }

    /**
     * Group of libraries at <b>resilience4j</b>
     */
    public Resilience4jLibraryAccessors getResilience4j() {
        return laccForResilience4jLibraryAccessors;
    }

    /**
     * Group of libraries at <b>spring</b>
     */
    public SpringLibraryAccessors getSpring() {
        return laccForSpringLibraryAccessors;
    }

    /**
     * Group of libraries at <b>springdoc</b>
     */
    public SpringdocLibraryAccessors getSpringdoc() {
        return laccForSpringdocLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class CamundaLibraryAccessors extends SubDependencyFactory {
        private final CamundaExternalLibraryAccessors laccForCamundaExternalLibraryAccessors = new CamundaExternalLibraryAccessors(owner);

        public CamundaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>camunda.external</b>
         */
        public CamundaExternalLibraryAccessors getExternal() {
            return laccForCamundaExternalLibraryAccessors;
        }

    }

    public static class CamundaExternalLibraryAccessors extends SubDependencyFactory {
        private final CamundaExternalTaskLibraryAccessors laccForCamundaExternalTaskLibraryAccessors = new CamundaExternalTaskLibraryAccessors(owner);

        public CamundaExternalLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>camunda.external.task</b>
         */
        public CamundaExternalTaskLibraryAccessors getTask() {
            return laccForCamundaExternalTaskLibraryAccessors;
        }

    }

    public static class CamundaExternalTaskLibraryAccessors extends SubDependencyFactory {

        public CamundaExternalTaskLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>client</b> with <b>org.camunda.bpm:camunda-external-task-client</b> coordinates and
         * with version reference <b>camundaExternalTask</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getClient() {
            return create("camunda.external.task.client");
        }

    }

    public static class JunitLibraryAccessors extends SubDependencyFactory {

        public JunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jupiter</b> with <b>org.junit.jupiter:junit-jupiter</b> coordinates and
         * with version reference <b>junit</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJupiter() {
            return create("junit.jupiter");
        }

    }

    public static class MockitoLibraryAccessors extends SubDependencyFactory {
        private final MockitoJunitLibraryAccessors laccForMockitoJunitLibraryAccessors = new MockitoJunitLibraryAccessors(owner);

        public MockitoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.mockito:mockito-core</b> coordinates and
         * with version reference <b>mockito</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("mockito.core");
        }

        /**
         * Group of libraries at <b>mockito.junit</b>
         */
        public MockitoJunitLibraryAccessors getJunit() {
            return laccForMockitoJunitLibraryAccessors;
        }

    }

    public static class MockitoJunitLibraryAccessors extends SubDependencyFactory {

        public MockitoJunitLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jupiter</b> with <b>org.mockito:mockito-junit-jupiter</b> coordinates and
         * with version reference <b>mockito</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJupiter() {
            return create("mockito.junit.jupiter");
        }

    }

    public static class Resilience4jLibraryAccessors extends SubDependencyFactory {
        private final Resilience4jSpringLibraryAccessors laccForResilience4jSpringLibraryAccessors = new Resilience4jSpringLibraryAccessors(owner);

        public Resilience4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>circuitbreaker</b> with <b>io.github.resilience4j:resilience4j-circuitbreaker</b> coordinates and
         * with version reference <b>resilience4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCircuitbreaker() {
            return create("resilience4j.circuitbreaker");
        }

        /**
         * Dependency provider for <b>retry</b> with <b>io.github.resilience4j:resilience4j-retry</b> coordinates and
         * with version reference <b>resilience4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRetry() {
            return create("resilience4j.retry");
        }

        /**
         * Group of libraries at <b>resilience4j.spring</b>
         */
        public Resilience4jSpringLibraryAccessors getSpring() {
            return laccForResilience4jSpringLibraryAccessors;
        }

    }

    public static class Resilience4jSpringLibraryAccessors extends SubDependencyFactory {

        public Resilience4jSpringLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>boot3</b> with <b>io.github.resilience4j:resilience4j-spring-boot3</b> coordinates and
         * with version reference <b>resilience4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBoot3() {
            return create("resilience4j.spring.boot3");
        }

    }

    public static class SpringLibraryAccessors extends SubDependencyFactory {
        private final SpringBootLibraryAccessors laccForSpringBootLibraryAccessors = new SpringBootLibraryAccessors(owner);

        public SpringLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>aspects</b> with <b>org.springframework:spring-aspects</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAspects() {
            return create("spring.aspects");
        }

        /**
         * Dependency provider for <b>retry</b> with <b>org.springframework.retry:spring-retry</b> coordinates and
         * with version reference <b>springRetry</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRetry() {
            return create("spring.retry");
        }

        /**
         * Group of libraries at <b>spring.boot</b>
         */
        public SpringBootLibraryAccessors getBoot() {
            return laccForSpringBootLibraryAccessors;
        }

    }

    public static class SpringBootLibraryAccessors extends SubDependencyFactory {
        private final SpringBootStarterLibraryAccessors laccForSpringBootStarterLibraryAccessors = new SpringBootStarterLibraryAccessors(owner);

        public SpringBootLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>spring.boot.starter</b>
         */
        public SpringBootStarterLibraryAccessors getStarter() {
            return laccForSpringBootStarterLibraryAccessors;
        }

    }

    public static class SpringBootStarterLibraryAccessors extends SubDependencyFactory {
        private final SpringBootStarterDataLibraryAccessors laccForSpringBootStarterDataLibraryAccessors = new SpringBootStarterDataLibraryAccessors(owner);

        public SpringBootStarterLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>actuator</b> with <b>org.springframework.boot:spring-boot-starter-actuator</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getActuator() {
            return create("spring.boot.starter.actuator");
        }

        /**
         * Dependency provider for <b>security</b> with <b>org.springframework.boot:spring-boot-starter-security</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSecurity() {
            return create("spring.boot.starter.security");
        }

        /**
         * Dependency provider for <b>test</b> with <b>org.springframework.boot:spring-boot-starter-test</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTest() {
            return create("spring.boot.starter.test");
        }

        /**
         * Dependency provider for <b>validation</b> with <b>org.springframework.boot:spring-boot-starter-validation</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getValidation() {
            return create("spring.boot.starter.validation");
        }

        /**
         * Dependency provider for <b>web</b> with <b>org.springframework.boot:spring-boot-starter-web</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getWeb() {
            return create("spring.boot.starter.web");
        }

        /**
         * Group of libraries at <b>spring.boot.starter.data</b>
         */
        public SpringBootStarterDataLibraryAccessors getData() {
            return laccForSpringBootStarterDataLibraryAccessors;
        }

    }

    public static class SpringBootStarterDataLibraryAccessors extends SubDependencyFactory {

        public SpringBootStarterDataLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>jpa</b> with <b>org.springframework.boot:spring-boot-starter-data-jpa</b> coordinates and
         * with <b>no version specified</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJpa() {
            return create("spring.boot.starter.data.jpa");
        }

    }

    public static class SpringdocLibraryAccessors extends SubDependencyFactory {
        private final SpringdocOpenapiLibraryAccessors laccForSpringdocOpenapiLibraryAccessors = new SpringdocOpenapiLibraryAccessors(owner);

        public SpringdocLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>springdoc.openapi</b>
         */
        public SpringdocOpenapiLibraryAccessors getOpenapi() {
            return laccForSpringdocOpenapiLibraryAccessors;
        }

    }

    public static class SpringdocOpenapiLibraryAccessors extends SubDependencyFactory {

        public SpringdocOpenapiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>starter</b> with <b>org.springdoc:springdoc-openapi-starter-webmvc-ui</b> coordinates and
         * with version reference <b>springdoc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getStarter() {
            return create("springdoc.openapi.starter");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>camundaExternalTask</b> with value <b>7.21.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCamundaExternalTask() { return getVersion("camundaExternalTask"); }

        /**
         * Version alias <b>camundaRestClient</b> with value <b>7.21.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getCamundaRestClient() { return getVersion("camundaRestClient"); }

        /**
         * Version alias <b>h2</b> with value <b>2.2.224</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getH2() { return getVersion("h2"); }

        /**
         * Version alias <b>jackson</b> with value <b>2.17.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJackson() { return getVersion("jackson"); }

        /**
         * Version alias <b>junit</b> with value <b>5.11.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getJunit() { return getVersion("junit"); }

        /**
         * Version alias <b>lombok</b> with value <b>1.18.34</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLombok() { return getVersion("lombok"); }

        /**
         * Version alias <b>mockito</b> with value <b>5.12.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMockito() { return getVersion("mockito"); }

        /**
         * Version alias <b>resilience4j</b> with value <b>2.2.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getResilience4j() { return getVersion("resilience4j"); }

        /**
         * Version alias <b>springBoot</b> with value <b>3.3.4</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSpringBoot() { return getVersion("springBoot"); }

        /**
         * Version alias <b>springDependencyMgmt</b> with value <b>1.1.6</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSpringDependencyMgmt() { return getVersion("springDependencyMgmt"); }

        /**
         * Version alias <b>springRetry</b> with value <b>2.0.7</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSpringRetry() { return getVersion("springRetry"); }

        /**
         * Version alias <b>springdoc</b> with value <b>2.6.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getSpringdoc() { return getVersion("springdoc"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {
        private final SpringPluginAccessors paccForSpringPluginAccessors = new SpringPluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Group of plugins at <b>plugins.spring</b>
         */
        public SpringPluginAccessors getSpring() {
            return paccForSpringPluginAccessors;
        }

    }

    public static class SpringPluginAccessors extends PluginFactory {
        private final SpringDepPluginAccessors paccForSpringDepPluginAccessors = new SpringDepPluginAccessors(providers, config);

        public SpringPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>spring.boot</b> with plugin id <b>org.springframework.boot</b> and
         * with version reference <b>springBoot</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getBoot() { return createPlugin("spring.boot"); }

        /**
         * Group of plugins at <b>plugins.spring.dep</b>
         */
        public SpringDepPluginAccessors getDep() {
            return paccForSpringDepPluginAccessors;
        }

    }

    public static class SpringDepPluginAccessors extends PluginFactory {

        public SpringDepPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>spring.dep.mgmt</b> with plugin id <b>io.spring.dependency-management</b> and
         * with version reference <b>springDependencyMgmt</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getMgmt() { return createPlugin("spring.dep.mgmt"); }

    }

}
