package com.ghazal.ims2.view.privacy;

import com.ghazal.ims2.view.common.PublicPageLanguage;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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

@AnonymousAllowed
@Route("privacy")
@RouteAlias("ar/privacy")
@RouteAlias("en/privacy")
@PageTitle("سياسة الخصوصية | Privacy Policy — Ghazal Electrical Contracting Company")
public class PrivacyPolicyView extends Div implements BeforeEnterObserver {

    private static final String PHONE = "092341936";
    private static final String EMAIL = "malath@ghazal.ps";

    private boolean english;

    public PrivacyPolicyView() {
        addClassName("privacy-page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String path = event.getLocation().getPath();
        english = PublicPageLanguage.isEnglish(path);
        PublicPageLanguage.apply(english, PublicPageLanguage.isExplicit(path));

        getElement().setAttribute("dir", english ? "ltr" : "rtl");
        getElement().setAttribute("lang", english ? "en" : "ar");

        removeAll();
        add(buildHeader());
        add(buildContent());
        add(buildFooter());
    }

    private String t(String arabic, String englishText) {
        return english ? englishText : arabic;
    }

    private String companyName() {
        return t("شركة غزال للمقاولات الكهربائية", "Ghazal Electrical Contracting Company");
    }

    private String phoneDisplay() {
        return t("092341936 (نابلس)", "092341936 (Nablus)");
    }

    private Component buildHeader() {
        Div header = new Div();
        header.addClassName("privacy-header");

        Div brand = new Div();
        brand.addClassName("privacy-brand");
        Span mark = new Span("غ");
        mark.addClassName("privacy-logo-mark");
        Span name = new Span(companyName());
        name.addClassName("privacy-logo-text");
        brand.add(mark, name);

        Anchor home = new Anchor();
        home.setHref(english ? "/en" : "/ar");
        home.setText(t("العودة إلى الصفحة الرئيسية", "Back to home"));
        home.addClassName("privacy-home-link");

        Anchor lang = new Anchor();
        lang.setHref(english ? "/ar/privacy" : "/en/privacy");
        lang.setText(english ? "العربية" : "English");
        lang.addClassName("privacy-lang-link");

        Div actions = new Div();
        actions.addClassName("privacy-header-actions");
        actions.add(home, lang);

        header.add(brand, actions);
        return header;
    }

    private Component buildContent() {
        Div container = new Div();
        container.addClassName("privacy-container");

        container.add(new H1(t("سياسة الخصوصية", "Privacy Policy")));

        container.add(new Paragraph(t(
                "تنطبق سياسة الخصوصية هذه على استخدام شركة غزال للمقاولات الكهربائية لمنصة واتساب "
                        + "للأعمال وللأنظمة الداخلية المرتبطة بها، والتي نستقبل عبرها أوامر العمل "
                        + "والطلبات من فريق العمل الميداني ونعالجها. وهي توضح أنواع البيانات التي "
                        + "نجمعها وكيفية استخدامها ومشاركتها وحمايتها.",
                "This privacy policy applies to Ghazal Electrical Contracting Company's use of the "
                        + "WhatsApp Business Platform and the related internal systems through which we "
                        + "receive and process work orders and requests from our field staff. It explains "
                        + "what data we collect and how we use, share, and protect it.")));

        container.add(heading(t("المعلومات التي نجمعها", "Information We Collect")));
        container.add(bullets(
                t("الاسم ورقم هاتف واتساب واسم الملف الشخصي.",
                        "Name, WhatsApp phone number, and WhatsApp profile name."),
                t("محتوى الرسائل: النصوص والرسائل الصوتية والصور والمستندات والمواقع التي تتم مشاركتها.",
                        "Message content: text, voice notes, images, documents, and any shared locations."),
                t("بيانات وصفية عن الرسائل مثل وقت الإرسال وحالة التسليم ومعرّفات الرسائل.",
                        "Message metadata such as timestamps, delivery status, and message identifiers."),
                t("بيانات تشغيلية مستخلصة من الرسائل مثل أوامر العمل والطلبات والمشاريع والمواقع.",
                        "Operational data derived from messages, such as work orders, requests, projects, "
                                + "and sites.")));

        container.add(heading(t("كيف نستخدم المعلومات", "How We Use Information")));
        container.add(bullets(
                t("استقبال أوامر العمل والطلبات الداخلية من الفريق الميداني ومعالجتها.",
                        "Receiving and processing internal work orders and requests from field staff."),
                t("إنشاء أوامر العمل والمهام ومتابعتها في أنظمتنا الداخلية.",
                        "Creating and tracking work orders and tasks in our internal systems."),
                t("فهم محتوى الرسائل وتصنيفها بمساعدة أنظمة الأتمتة والذكاء الاصطناعي.",
                        "Understanding and classifying message content with the help of automation and "
                                + "AI systems."),
                t("توثيق سير العمل ومراجعة الجودة والاحتفاظ بالسجلات.",
                        "Documenting workflows, quality review, and record keeping."),
                t("الامتثال للالتزامات القانونية والتنظيمية.",
                        "Complying with legal and regulatory obligations.")));

        container.add(heading(t("المعالجة الآلية والذكاء الاصطناعي",
                "Automated Processing and AI")));
        container.add(new Paragraph(t(
                "قد تتم معالجة الرسائل عبر أنظمة أتمتة داخلية (مثل سير عمل n8n) وبمساعدة خدمات "
                        + "ذكاء اصطناعي تعمل لحسابنا، وذلك لفهم محتوى الرسالة وتحويلها إلى أمر عمل "
                        + "أو مهمة. ويبقى القرار النهائي بمراجعة بشرية من فريقنا.",
                "Messages may be processed by our internal automation systems (such as self-hosted n8n "
                        + "workflows) and with the assistance of AI services acting on our behalf, in "
                        + "order to understand the message content and turn it into a work order or task. "
                        + "Final decisions remain subject to human review by our team.")));

        container.add(heading(t("مشاركة المعلومات", "Information Sharing")));
        container.add(bullets(
                t("تُعالج الرسائل عبر منصة واتساب للأعمال التابعة لشركة Meta، وتخضع لسياسات Meta وواتساب.",
                        "Messages are processed through Meta's WhatsApp Business Platform and are subject "
                                + "to Meta's and WhatsApp's policies."),
                t("قد نستعين بمزودي خدمات الأتمتة والذكاء الاصطناعي والاستضافة لمعالجة البيانات نيابة "
                        + "عنا وبموجب اتفاقيات تحفظ السرية.",
                        "We may use automation, AI, and hosting providers to process data on our behalf "
                                + "under confidentiality agreements."),
                t("لا نبيع بياناتك ولا نشاركها مع أطراف ثالثة لأغراض تسويقية.",
                        "We do not sell your data or share it with third parties for marketing purposes."),
                t("قد نفصح عن البيانات عند وجود التزام قانوني يقتضي ذلك.",
                        "We may disclose data where a legal obligation requires it.")));

        container.add(heading(t("الاحتفاظ بالبيانات", "Data Retention")));
        container.add(new Paragraph(t(
                "نحتفظ بالبيانات للمدة اللازمة لتحقيق الأغراض المذكورة أعلاه أو للامتثال "
                        + "للالتزامات القانونية، ثم يتم حذفها أو إتلافها بشكل آمن.",
                "We retain data for as long as necessary to achieve the purposes above or to comply with "
                        + "legal obligations, after which it is securely deleted or destroyed.")));

        container.add(heading(t("أمن البيانات", "Data Security")));
        container.add(new Paragraph(t(
                "نستخدم إجراءات تقنية وتنظيمية مناسبة لحماية البيانات من الوصول غير المصرح به، بما "
                        + "في ذلك ضبط صلاحيات الوصول والتشفير أثناء النقل.",
                "We use appropriate technical and organizational measures to protect data from "
                        + "unauthorized access, including access controls and encryption in transit.")));

        container.add(heading(t("حقوقك", "Your Rights")));
        container.add(new Paragraph(t(
                "يمكنك في أي وقت طلب الاطلاع على بياناتك أو تصحيحها أو حذفها بالتواصل معنا عبر "
                        + "بيانات الاتصال الواردة أدناه.",
                "You may request access to, correction of, or deletion of your data at any time by "
                        + "contacting us using the details below.")));

        container.add(heading(t("طلب حذف البيانات", "Data Deletion Request")));
        container.add(new Paragraph(t(
                "لطلب حذف رسائلك وبياناتك المرتبطة بها من أنظمتنا، راسلنا على البريد الإلكتروني "
                        + EMAIL + " أو الهاتف " + phoneDisplay() + " مع ذكر رقم واتساب الخاص بك. "
                        + "سنتحقق من الطلب وننفذه خلال مدة معقولة، مع الاحتفاظ بما يقتضي القانون "
                        + "الاحتفاظ به. يرجى ملاحظة أن نسخًا من الرسائل قد تبقى على جهازك أو داخل "
                        + "تطبيق واتساب، وهي خارج نطاق سيطرتنا.",
                "To request deletion of your messages and related data from our systems, contact us by "
                        + "email at " + EMAIL + " or by phone at " + phoneDisplay() + " and include your "
                        + "WhatsApp number. We will verify and fulfil the request within a reasonable "
                        + "period, retaining only what the law requires us to keep. Please note that "
                        + "copies of messages may remain on your device or inside the WhatsApp "
                        + "application, which are outside our control.")));

        container.add(heading(t("التغييرات على هذه السياسة", "Changes to This Policy")));
        container.add(new Paragraph(t(
                "قد نقوم بتحديث هذه السياسة من وقت لآخر، ويُعد استمرارك في استخدام خدماتنا موافقة "
                        + "على النسخة المحدثة.",
                "We may update this policy from time to time, and your continued use of our services "
                        + "constitutes acceptance of the updated version.")));

        container.add(heading(t("الاتصال بنا", "Contact Us")));
        container.add(new Paragraph(t(
                "شركة غزال للمقاولات الكهربائية — الفرع الرئيسي: نابلس، منطقة رفيديا. "
                        + "الهاتف: " + phoneDisplay() + " — البريد الإلكتروني: " + EMAIL + ".",
                "Ghazal Electrical Contracting Company — Main branch: Nablus, Rafidia area. Phone: "
                        + phoneDisplay() + " — Email: " + EMAIL + ".")));

        return container;
    }

    private Component buildFooter() {
        Div footer = new Div();
        footer.addClassName("privacy-footer");
        footer.add(new Span("© " + java.time.Year.now().getValue() + " " + companyName()));
        return footer;
    }

    private H2 heading(String text) {
        return new H2(text);
    }

    private UnorderedList bullets(String... items) {
        UnorderedList list = new UnorderedList();
        for (String item : items) {
            list.add(new ListItem(item));
        }
        return list;
    }
}
