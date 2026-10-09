package com.ghazal.ims2.view.landing;

import com.ghazal.ims2.view.common.PublicPageLanguage;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@AnonymousAllowed
@Route("")
@RouteAlias("ar")
@RouteAlias("en")
@PageTitle("شركة غزال للمقاولات الكهربائية | Ghazal Electrical Contracting Company")
public class LandingView extends Div implements BeforeEnterObserver {

    private static final String PHONE = "092341936";
    private static final String EMAIL = "malath@ghazal.ps";
    private static final String FOUNDER_PHOTO = "/images/ibrahim-ghazal.webp";

    @Autowired
    private CurrentAuthentication currentAuthentication;

    private boolean english;

    public LandingView() {
        addClassName("landing-page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var authentication = currentAuthentication.getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            event.forwardTo("main");
            return;
        }

        String path = event.getLocation().getPath();
        english = PublicPageLanguage.isEnglish(path);
        PublicPageLanguage.apply(english, PublicPageLanguage.isExplicit(path));

        getElement().setAttribute("dir", english ? "ltr" : "rtl");
        getElement().setAttribute("lang", english ? "en" : "ar");

        removeAll();
        add(buildHeader());
        add(buildHero());
        add(buildAbout());
        add(buildFounder());
        add(buildContact());
        add(buildFooter());
    }

    private String t(String arabic, String englishText) {
        return english ? englishText : arabic;
    }

    private String companyName() {
        return t("شركة غزال للمقاولات الكهربائية", "Ghazal Electrical Contracting Company");
    }

    private String branchTitle() {
        return t("الفرع الرئيسي (نابلس)", "Main Branch (Nablus)");
    }

    private String branchDescription() {
        return t("تعمل تحت اسم شركة غزال للمقاولات الكهربائية وتقع في منطقة رفيديا.",
                "Operating under the name Ghazal Electrical Contracting Company and located in the Rafidia area.");
    }

    private String phoneDisplay() {
        return t("092341936 (نابلس)", "092341936 (Nablus)");
    }

    private String addressValue() {
        return t("رفيديا، نابلس، فلسطين", "Rafidia, Nablus, Palestine");
    }

    private Component buildHeader() {
        Div header = new Div();
        header.addClassName("landing-header");

        Div logo = new Div();
        logo.addClassName("landing-logo");
        Span mark = new Span("غ");
        mark.addClassName("landing-logo-mark");
        Span name = new Span(companyName());
        name.addClassName("landing-logo-text");
        logo.add(mark, name);

        Div nav = new Div();
        nav.addClassName("landing-header-nav");
        nav.add(anchor("#about", t("من نحن", "About Us"), null));
        nav.add(anchor("#founder", t("المؤسس", "Founder"), null));
        nav.add(anchor("#contact", t("اتصل بنا", "Contact Us"), null));

        Div actions = new Div();
        actions.addClassName("landing-header-actions");
        actions.add(langAnchor());
        actions.add(loginAnchor(t("تسجيل الدخول", "Login"), "landing-btn landing-btn-login"));

        header.add(logo, nav, actions);
        return header;
    }

    private Component buildHero() {
        Div hero = new Div();
        hero.addClassName("landing-hero");

        Span eyebrow = new Span(t("مقاولات كهربائية · نابلس · فلسطين",
                "Electrical Contracting · Nablus · Palestine"));
        eyebrow.addClassName("landing-eyebrow");

        H1 title = new H1(t("طاقة تُبنى بثقة", "Power Built on Trust"));
        title.addClassName("landing-hero-title");

        Paragraph lead = new Paragraph(t(
                "حلول كهربائية متكاملة للمشاريع السكنية والتجارية والصناعية، من التخطيط والتنفيذ "
                        + "إلى الصيانة والتشغيل، بمعايير جودة عالية وفريق متخصص.",
                "Integrated electrical solutions for residential, commercial, and industrial projects — "
                        + "from planning and execution to maintenance and operation, with high quality "
                        + "standards and a specialized team."));
        lead.addClassName("landing-hero-lead");

        Div actions = new Div();
        actions.addClassName("landing-hero-actions");
        actions.add(loginAnchor(t("الدخول إلى النظام", "Login to the System"), "landing-btn landing-btn-primary"));
        actions.add(anchor("#contact", t("تواصل معنا", "Contact Us"), "landing-btn landing-btn-ghost"));

        hero.add(eyebrow, title, lead, actions);
        return hero;
    }

    private Component buildAbout() {
        Div inner = new Div();
        inner.addClassName("landing-about-grid");

        Div text = new Div();
        text.addClassName("landing-about-text");
        text.add(new Paragraph(t(
                "شركة غزال للمقاولات الكهربائية شركة فلسطينية متخصصة في تنفيذ الأعمال الكهربائية "
                        + "للمشاريع السكنية والتجارية والصناعية. نعمل على تقديم حلول آمنة وموثوقة "
                        + "تلبي احتياجات عملائنا من مرحلة التصميم حتى التسليم النهائي.",
                "Ghazal Electrical Contracting Company is a Palestinian company specialized in executing "
                        + "electrical works for residential, commercial, and industrial projects. We deliver "
                        + "safe and reliable solutions that meet our clients' needs from design through final "
                        + "handover.")));
        text.add(new Paragraph(t(
                "نؤمن بأن جودة التنفيذ تبدأ من الالتزام بالمعايير الهندسية والشفافية مع العميل، "
                        + "لذلك نحرص على متابعة كل مشروع بدقة والالتزام بالمواعيد المتفق عليها.",
                "We believe that quality starts with adherence to engineering standards and transparency "
                        + "with the client, so we carefully follow up every project and commit to the agreed "
                        + "timelines.")));

        Div branch = new Div();
        branch.addClassName("landing-branch-card");
        H3 branchTitle = new H3(branchTitle());
        branchTitle.addClassName("landing-branch-title");
        branch.add(branchTitle, new Paragraph(branchDescription()));

        inner.add(text, branch);
        return section("about", t("من نحن", "About Us"), inner);
    }

    private Component buildFounder() {
        Div inner = new Div();
        inner.addClassName("landing-founder-grid");

        Image photo = new Image(FOUNDER_PHOTO,
                t("م. إبراهيم غزال — مؤسس شركة غزال للمقاولات الكهربائية",
                        "Eng. Ibrahim Ghazal — Founder of Ghazal Electrical Contracting Company"));
        photo.addClassName("landing-founder-photo");

        Div text = new Div();
        text.addClassName("landing-founder-text");

        H3 name = new H3(t("م. إبراهيم غزال", "Eng. Ibrahim Ghazal"));
        name.addClassName("landing-founder-name");

        Span role = new Span(t("مؤسس شركة غزال للمقاولات الكهربائية",
                "Founder of Ghazal Electrical Contracting Company"));
        role.addClassName("landing-founder-role");

        Paragraph bio = new Paragraph(t(
                "أسس م. إبراهيم غزال شركة غزال للمقاولات الكهربائية، ويمتلك خبرة طويلة في مجال "
                        + "المقاولات الكهربائية. درس الهندسة في بيلاروسيا، وأنجز خلال مسيرته مئات "
                        + "المشاريع الكبرى، وكان مندوبًا لاتحاد المقاولين الفلسطينيين.",
                "Eng. Ibrahim Ghazal founded Ghazal Electrical Contracting Company and has extensive "
                        + "experience in the electrical contracting field. He studied engineering in Belarus "
                        + "and has delivered hundreds of major projects throughout his career. He also served "
                        + "as a representative of the Palestinian Contractors Union."));

        UnorderedList facts = new UnorderedList();
        facts.addClassName("landing-founder-facts");
        facts.add(new ListItem(t("درس الهندسة في بيلاروسيا", "Studied engineering in Belarus")));
        facts.add(new ListItem(t("خبرة طويلة تمتد لعشرات السنين", "Extensive experience spanning decades")));
        facts.add(new ListItem(t("أنجز مئات المشاريع الكبرى", "Completed hundreds of major projects")));
        facts.add(new ListItem(t("كان مندوب اتحاد المقاولين الفلسطينيين",
                "Represented the Palestinian Contractors Union")));

        text.add(name, role, bio, facts);

        inner.add(photo, text);
        return section("founder", t("المؤسس", "Founder"), inner);
    }

    private Component buildContact() {
        Div grid = new Div();
        grid.addClassName("landing-contact-grid");

        grid.add(contactCard("📞", t("الهاتف", "Phone"), phoneDisplay(),
                anchor("tel:" + PHONE, phoneDisplay(), null)));
        grid.add(contactCard("✉️", t("البريد الإلكتروني", "Email"), EMAIL,
                anchor("mailto:" + EMAIL, EMAIL, null)));
        grid.add(contactCard("📍", t("العنوان", "Address"), addressValue(), null));

        return section("contact", t("اتصل بنا", "Contact Us"), grid);
    }

    private Component contactCard(String icon, String title, String value, Component link) {
        Div card = new Div();
        card.addClassName("landing-contact-card");

        Span iconSpan = new Span(icon);
        iconSpan.addClassName("landing-contact-icon");

        H3 cardTitle = new H3(title);
        cardTitle.addClassName("landing-contact-title");

        card.add(iconSpan, cardTitle);
        card.add(link != null ? link : new Span(value));
        return card;
    }

    private Component buildFooter() {
        Div footer = new Div();
        footer.addClassName("landing-footer");

        Div brand = new Div();
        brand.addClassName("landing-footer-brand");
        brand.add(new Span(companyName()));
        brand.add(new Span(branchTitle()));

        Div right = new Div();
        right.addClassName("landing-footer-right");
        right.add(new Span("© " + java.time.Year.now().getValue() + " " + companyName()));
        right.add(loginAnchor(t("تسجيل الدخول", "Login"), "landing-footer-link"));

        footer.add(brand, right);
        return footer;
    }

    private Component section(String id, String title, Component... body) {
        Div section = new Div();
        section.setId(id);
        section.addClassName("landing-section");

        Div inner = new Div();
        inner.addClassName("landing-container");

        H2 heading = new H2(title);
        heading.addClassName("landing-section-title");
        inner.add(heading);
        inner.add(body);

        section.add(inner);
        return section;
    }

    private Anchor langAnchor() {
        Anchor anchor = new Anchor();
        anchor.setHref(english ? "/ar" : "/en");
        anchor.setText(english ? "العربية" : "English");
        anchor.addClassName("landing-lang-link");
        return anchor;
    }

    private Anchor loginAnchor(String text, String className) {
        Anchor anchor = anchor("login", text, className);
        anchor.addClassName("landing-login-link");
        return anchor;
    }

    private Anchor anchor(String href, String text, String className) {
        Anchor anchor = new Anchor();
        anchor.setHref(href);
        anchor.setText(text);
        if (className != null) {
            anchor.addClassNames(className.split("\\s+"));
        }
        return anchor;
    }
}
