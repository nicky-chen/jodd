// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.joy;

import jodd.petite.AutomagicPetiteConfigurator;
import jodd.petite.PetiteContainer;
import jodd.petite.proxetta.ProxettaAwarePetiteContainer;
import jodd.petite.scope.SessionScope;
import jodd.petite.scope.SingletonScope;
import jodd.props.Props;
import jodd.proxetta.impl.ProxyProxetta;
import jodd.util.Consumers;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static jodd.joy.JoddJoy.PETITE_CORE;
import static jodd.joy.JoddJoy.PETITE_SCAN;

public class JoyPetite extends JoyBase {

	protected final Supplier<JoyScanner> joyScannerSupplier;
	protected final Supplier<Props> propsSupplier;
	protected final Supplier<ProxyProxetta> proxettaSupplier;
	protected final Config config;

	protected PetiteContainer petiteContainer;
	protected boolean isWebApplication = true;  // todo add this value as well!

	public JoyPetite(
		Supplier<ProxyProxetta> proxettaSupplier,
		Supplier<Props> propsSupplier, Supplier<JoyScanner> joyScannerSupplier) {
		this.proxettaSupplier = proxettaSupplier;
		this.joyScannerSupplier = joyScannerSupplier;
		this.propsSupplier = propsSupplier;
		this.config = new Config();
	}

	public PetiteContainer petiteContainer() {
		return petiteContainer;
	}

	public Config config() {
		return config;
	}

	public class Config {
		private boolean autoConfiguration = true;
		private Consumers<PetiteContainer> petiteContainerConsumers = Consumers.empty();

		public Config disableAutoConfiguration() {
			autoConfiguration = false;
			return this;
		}

		public Config withPetite(Consumer<PetiteContainer> petiteContainerConsumer) {
			petiteContainerConsumers.add(petiteContainerConsumer);
			return this;
		}
	}

	/**
	 * Creates and initializes Petite container.
	 * It will be auto-magically configured by scanning the classpath.
	 * Also, all 'app*.prop*' will be loaded and values will
	 * be injected in the matched beans. At the end it registers
	 * this instance of core into the container.
	 */
	@Override
	public void start() {
		initLogger();

		log.info("PETITE start  ----------");

		petiteContainer = createPetiteContainer();

		log.info("app in web: " + isWebApplication);

		if (!isWebApplication) {
			// make session scope to act as singleton scope
			// if this is not a web application (and http session is not available).
			petiteContainer.registerScope(SessionScope.class, new SingletonScope());
		}

		// load parameters from properties files
		petiteContainer.defineParameters(propsSupplier.get());

		petiteContainer.addBean(PETITE_SCAN, joyScannerSupplier.get());

		// automagic configuration
		if (config.autoConfiguration) {
			log.info("*PETITE Automagic scanning");

			registerPetiteContainerBeans(petiteContainer);
		}

		log.debug("Petite manual configuration started...");
		config.petiteContainerConsumers.accept(this.petiteContainer);

		// add AppCore instance to Petite
		petiteContainer.addBean(PETITE_CORE, petiteContainer);
	}

	protected ProxettaAwarePetiteContainer createPetiteContainer() {
		return new ProxettaAwarePetiteContainer(proxettaSupplier.get());
	}

	/**
	 * Configures Petite container. By default scans the class path
	 * for petite beans and registers them automagically.
	 */
	protected void registerPetiteContainerBeans(PetiteContainer petiteContainer) {
		AutomagicPetiteConfigurator pcfg = new AutomagicPetiteConfigurator();

		pcfg.withScanner(classScanner -> joyScannerSupplier.get().accept(classScanner));

		pcfg.configure(petiteContainer);
	}

	/**
	 * Stops Petite container.
	 */
	@Override
	public void stop() {
		if (log != null) {
			log.info("PETITE stop");
		}
		if (petiteContainer != null) {
			petiteContainer.shutdown();
		}
	}
}
