package ru.inhell.aida.web;

import org.apache.wicket.Page;
import org.apache.wicket.atmosphere.EventBus;
import org.apache.wicket.protocol.http.WebApplication;
import org.wicketstuff.javaee.injection.JavaEEComponentInjector;
import ru.inhell.aida.common.service.GlassfishJndiNamingStrategy;
import ru.inhell.aida.template.test.HelloMenu;

/**
 * @author Anatoly A. Ivanov java@inhell.ru
 *         Date: 25.07.11 23:09
 */
public class AidaWebApplication extends WebApplication{
    @Override
    protected void init() {
        super.init();

        new EventBus(this);

        getComponentInstantiationListeners().add(new JavaEEComponentInjector(this, new GlassfishJndiNamingStrategy()));

        mountPage("/test", HelloMenu.class);
    }

    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }
}
