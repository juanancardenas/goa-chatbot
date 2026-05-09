package es.upm.api.domain.common;

public final class ChatbotResponseMessages {
    private ChatbotResponseMessages() {
    }

    //ChatbotService

    public static final String CLIENT_GENERAL_START_REPLY =
            "Hola. Soy tu asistente virtual y puedo ayudarte con dudas sobre GOA, tu encargo y los próximos pasos.";
    public static final String PROFESSIONAL_GENERAL_START_REPLY =
            "Conversación iniciada. Puedes consultar dudas operativas, funcionales y de gestión relacionadas con GOA y los encargos.";

    public static final String CLIENT_MESSAGE_REPLY =
            "He recibido tu mensaje. Voy a ayudarte con una respuesta clara y práctica.";
    public static final String PROFESSIONAL_MESSAGE_REPLY =
            "Mensaje recibido. Te respondo con orientación operativa y de gestión dentro del alcance de la plataforma.";

    public static final String CLIENT_GENERAL_STATUS_REPLY =
            "Puedo ayudarte con el estado general de un encargo. Si quieres información de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String PROFESSIONAL_GENERAL_STATUS_REPLY =
            "Puedo orientarte sobre el estado de un encargo. Si necesitas datos confirmados de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String CLIENT_GENERAL_TIMELINE_REPLY =
            "Puedo orientarte sobre próximos pasos, hitos o plazos. Si quieres información concreta de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String PROFESSIONAL_GENERAL_TIMELINE_REPLY =
            "Puedo orientar sobre próximos pasos, hitos o plazos. Si necesitas datos verificables de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String CLIENT_GENERAL_DOCUMENTS_REPLY =
            "Puedo resolver dudas generales sobre documentación. Si quieres revisar documentos concretos de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String PROFESSIONAL_GENERAL_DOCUMENTS_REPLY =
            "Puedo resolver dudas generales sobre documentación. Si necesitas documentos concretos de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String CLIENT_GENERAL_CONTEXT_REPLY =
            "Claro. Puedo ayudarte con consultas generales sobre GOA, gestión de encargos, pasos habituales, tareas legales orientativas, documentación y dudas operativas.\n" +
            " Si preguntas por datos reales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String PROFESSIONAL_GENERAL_CONTEXT_REPLY =
            "Puedo ayudarte con consultas generales sobre GOA, gestión operativa de encargos, procedimientos, tareas legales orientativas, documentación y criterios de uso del asistente.\n" +
            " Si la consulta requiere datos reales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String CLIENT_CONTEXTUAL_STATUS_REPLY_TEMPLATE =
            "He revisado la hoja de encargo %s. Este encargo está asociado a %s y puedo explicarte el caso con la información disponible en plataforma. ¿Quieres que empecemos por estado, hitos o tareas legales?";

    public static final String PROFESSIONAL_CONTEXTUAL_STATUS_REPLY_TEMPLATE =
            "He revisado la hoja de encargo %s, asociada a %s. Puedo apoyarme en la información operativa del caso para responder con precisión. Si te parece, puedo continuar con estado, hitos o tareas legales.";

    public static final String CLIENT_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE =
            "Los procedimientos visibles relacionados con este encargo son: %s.";

    public static final String PROFESSIONAL_CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE =
            "Los procedimientos visibles asociados a este encargo son: %s.";

    public static final String CLIENT_CONTEXTUAL_EVENTS_REPLY_TEMPLATE =
            "Los hitos o pasos recientes visibles del encargo son: %s.";

    public static final String PROFESSIONAL_CONTEXTUAL_EVENTS_REPLY_TEMPLATE =
            "Los hitos o eventos recientes visibles del encargo son: %s.";

    public static final String CLIENT_CONTEXTUAL_NO_EVENTS_REPLY =
            "No se han encontrado hitos recientes visibles en este momento para el encargo activo. Si quieres, revisamos ahora el estado general o las tareas legales visibles.";

    public static final String PROFESSIONAL_CONTEXTUAL_NO_EVENTS_REPLY =
            "No se han encontrado hitos recientes visibles en este momento para el encargo activo. Si te sirve, puedo continuar con estado general o tareas legales visibles.";

    public static final String CLIENT_CONTEXTUAL_DOCUMENTS_REPLY =
            "Tu consulta parece referirse a documentos del caso. En esta fase solo debo apoyarme en documentación autorizada y visible, sin asumir documentos no confirmados. Si quieres, te ayudo con lo que sí está visible en el encargo activo.";

    public static final String PROFESSIONAL_CONTEXTUAL_DOCUMENTS_REPLY =
            "Tu consulta parece referirse a documentación del caso. En esta fase la respuesta debe apoyarse solo en documentación autorizada y visible, sin inventar documentos ni confirmar accesos no verificados. Si te parece, seguimos con el contexto operativo disponible.";

    public static final String CLIENT_CONTEXTUAL_GENERAL_SUMMARY_REPLY =
            "He cargado el contexto del encargo activo y puedo ayudarte con una explicación general del caso, sus procedimientos visibles y los hitos recientes disponibles en plataforma. ¿Quieres que priorice estado, hitos o tareas legales?";

    public static final String PROFESSIONAL_CONTEXTUAL_GENERAL_SUMMARY_REPLY =
            "He cargado contexto del encargo activo y puedo ayudarte con información general del caso, sus procedimientos visibles y los hitos recientes disponibles en plataforma. Si quieres, priorizo estado, hitos o tareas legales.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_STATUS_REPLY =
            "Ahora mismo no he podido recuperar el estado actualizado del encargo activo. Si te parece, vuelve a intentarlo en unos momentos y, mientras tanto, te ayudo con una orientación general del siguiente paso.";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_STATUS_REPLY =
            "No he podido recuperar ahora el contexto actualizado del encargo activo para responder sobre su estado. Prefiero no darte datos sin confirmación de plataforma; si quieres, te doy una orientación operativa mientras se restablece.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_EVENTS_REPLY =
            "Ahora mismo no he podido recuperar los hitos o pasos recientes del encargo activo. Puedes volver a intentarlo en unos momentos; si quieres, continuamos con estado general o tareas legales.";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_EVENTS_REPLY =
            "No he podido recuperar ahora los hitos o eventos recientes del encargo activo. Prefiero no dar una cronología como confirmada sin datos de plataforma; si te sirve, seguimos con estado general o tareas legales.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_DOCUMENTS_REPLY =
            "Ahora mismo no he podido comprobar la documentación visible del caso. Solo debo apoyarme en documentos autorizados y confirmados por la plataforma. Si quieres, te ayudo con el resto del contexto operativo disponible.";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_REPLY =
            "No he podido comprobar ahora la documentación visible del caso. Para evitar errores, no voy a afirmar accesos ni documentos no confirmados. Si te parece, seguimos con el contexto operativo que sí está disponible.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_GENERAL_REPLY =
            "Ahora mismo no he podido cargar el contexto actualizado del encargo activo. Puedo seguir ayudándote con orientación general, pero sin asumir datos confirmados del caso. ¿Quieres que te proponga los siguientes pasos habituales?";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_GENERAL_REPLY =
            "No he podido cargar ahora el contexto actualizado del encargo activo. Puedo continuar con orientación general, pero sin tratar como confirmados datos internos no disponibles. Si quieres, te propongo una secuencia operativa de seguimiento.";

    public static final String CLIENT_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE =
            """
            Las Tareas Legales visibles asociadas a este encargo son:
    
            %s
            """;

    public static final String PROFESSIONAL_CONTEXTUAL_LEGAL_TASKS_REPLY_TEMPLATE =
            """
            Las Tareas Legales visibles asociadas a este encargo son:
    
            %s
            """;

    public static final String CLIENT_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY =
            """
            Puedo ayudarte con una orientación general sobre tareas legales típicas de un encargo.
            Si necesitas datos reales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.
            En esta versión no puedo mostrar tablas o gráficos en la interfaz, pero sí puedo darte una lista orientativa clara.
            """;

    public static final String PROFESSIONAL_GENERAL_LEGAL_TASKS_EXAMPLE_REPLY =
            """
            Puedes responder de forma general sobre Legal Tasks habituales de un encargo jurídico.
            Si el usuario pide tabla, gráfico o formato Markdown, indica que de momento no está disponible en la interfaz y ofrece una lista orientativa.
            No presentes esa información como datos reales de un encargo concreto.
            """;

    public static final String CLIENT_GENERAL_STATUS_EXAMPLE_REPLY =
            """
            Puedo explicar de forma general qué estados puede tener un encargo y qué suele significar cada uno.
            Si necesitas el estado real de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.
            """;

    public static final String PROFESSIONAL_GENERAL_STATUS_EXAMPLE_REPLY =
            """
            Puedes explicar de forma general los estados habituales de un encargo y su significado operativo.
            No presentes ningún estado como real si no existe conversación contextual con datos de plataforma.
            """;

    public static final String CLIENT_GENERAL_TIMELINE_EXAMPLE_REPLY =
            """
            Puedo explicar de forma general qué hitos o pasos suelen aparecer en un encargo.
            Si necesitas fechas o próximos pasos reales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.
            """;

    public static final String PROFESSIONAL_GENERAL_TIMELINE_EXAMPLE_REPLY =
            """
            Puedes describir de forma general hitos, eventos o pasos habituales de un encargo jurídico.
            No presentes fechas, plazos o eventos como reales si no provienen de contexto de plataforma.
            """;

    public static final String CLIENT_CONTEXTUAL_NO_LEGAL_TASKS_REPLY =
            "No se han encontrado Tareas Legales visibles en este momento para el encargo activo. Si quieres, revisamos estado general o hitos recientes.";

    public static final String PROFESSIONAL_CONTEXTUAL_NO_LEGAL_TASKS_REPLY =
            "No se han encontrado Tareas Legales visibles en este momento para el encargo activo. Si te sirve, continuamos con estado general o hitos recientes.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY =
            "Ahora mismo no he podido recuperar las Tareas Legales del encargo activo. Puedes volver a intentarlo en unos momentos; si quieres, seguimos con estado o hitos del encargo activo.";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_LEGAL_TASKS_REPLY =
            "No he podido recuperar ahora las Tareas Legales del encargo activo. Prefiero no listarlas como confirmadas sin datos de plataforma; si te parece, avanzamos con estado o hitos.";

    public static final String CLIENT_GENERAL_LEGAL_TASKS_REPLY =
            "Para consultar tareas legales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String PROFESSIONAL_GENERAL_LEGAL_TASKS_REPLY =
            "Para consultar Tareas Legales de un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String CLIENT_CONTEXTUAL_DOCUMENTS_STUB_REPLY =
            "Tu consulta parece referirse a documentos del caso. La integración documental real aún no está disponible en esta versión, así que solo puedo orientarte de forma segura sin afirmar documentos concretos no confirmados. Si quieres, seguimos con el contexto operativo del encargo activo.";

    public static final String PROFESSIONAL_CONTEXTUAL_DOCUMENTS_STUB_REPLY =
            "Tu consulta parece referirse a documentación del caso. La integración documental real aún no está disponible en esta versión, así que no puedo confirmar documentos concretos ni accesos no verificados. Si te parece, continúo con la información operativa disponible del encargo activo.";

    public static final String CLIENT_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY =
            "Todavía no dispongo de integración documental real para comprobar documentos visibles del caso. Puedo orientarte de forma segura, sin inventar información. ¿Quieres que sigamos con estado, hitos o tareas?";

    public static final String PROFESSIONAL_CONTEXT_UNAVAILABLE_DOCUMENTS_STUB_REPLY =
            "La integración documental real aún no está disponible en esta versión. Por ahora no puedo confirmar accesos documentales, documentos visibles ni metadatos no verificados. Si te sirve, seguimos con el resto del contexto operativo.";

    public static final String CLIENT_GENERAL_DOCUMENTS_STUB_REPLY =
            "Puedo orientarte de forma general sobre documentación, pero en esta versión todavía no tengo integración documental real para consultar documentos concretos del caso.";

    public static final String PROFESSIONAL_GENERAL_DOCUMENTS_STUB_REPLY =
            "Puedo orientar de forma general sobre documentación, pero en esta versión todavía no dispongo de integración documental real para consultar documentos concretos del caso.";

    // Chat contextual

    public static final String CONTEXTUAL_PLATFORM_DATA_REPLY_TEMPLATE =
            "He revisado la hoja de encargo %s. El encargo está asociado a %s y puedo apoyarme en información interna disponible del caso para responder con más precisión.";

    public static final String CONTEXTUAL_PLATFORM_DATA_PROCEDURES_TEMPLATE =
            "Los procedimientos visibles en este encargo incluyen: %s.";

    public static final String CONTEXTUAL_PLATFORM_DATA_UNAVAILABLE_REPLY =
            "No he podido recuperar ahora el contexto actualizado del encargo activo. Puedo seguir ayudándote, pero sin presentar como confirmados datos internos del caso. Si quieres, te propongo cómo continuar en cuanto se recupere el contexto.";

    public static final String CONTEXTUAL_PLATFORM_EVENTS_TEMPLATE =
            "Los hitos o eventos recientes visibles del encargo son: %s.";

    public static final String CONTEXTUAL_PLATFORM_NO_EVENTS_TEMPLATE =
            "No se han encontrado hitos recientes visibles en este momento para el encargo activo.";

    public static final String CONTEXTUAL_STATUS_REPLY_TEMPLATE =
            "He revisado la hoja de encargo %s, asociada a %s. Puedo apoyarme en la información general del caso para darte una respuesta más precisa.";

    public static final String CONTEXTUAL_PROCEDURES_REPLY_TEMPLATE =
            "Los procedimientos visibles asociados a este encargo son: %s.";

    public static final String CONTEXTUAL_EVENTS_REPLY_TEMPLATE =
            "Los hitos o eventos recientes visibles del encargo son: %s.";

    public static final String CONTEXTUAL_NO_EVENTS_REPLY =
            "Ahora mismo no veo hitos recientes visibles para este encargo en el contexto recuperado. Si te parece, revisamos estado general o tareas legales visibles.";

    public static final String CONTEXTUAL_DOCUMENTS_REPLY =
            "Tu consulta parece referirse a documentación del caso. En esta fase la respuesta debe apoyarse solo en documentación autorizada y visible, sin inventar documentos ni confirmar accesos no verificados. Si quieres, continuamos con estado, hitos o tareas del encargo activo.";

    public static final String CONTEXTUAL_GENERAL_SUMMARY_REPLY =
            "He cargado contexto del encargo activo y puedo ayudarte con información general del caso, sus procedimientos visibles y los hitos recientes disponibles en plataforma. Si quieres, te detallo el punto que prefieras primero.";


    //ScopePolicy

    public static final String MISSING_CASE_CONTEXT_REPLY =
            "Esta conversación es general y no está asociada a un encargo específico. Si quieres estado, documentos o pasos de un caso, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String OUT_OF_CASE_SCOPE_REPLY =
            "Solo puedo responder dentro del ámbito del encargo activo. Si necesitas consultar otro caso, abre una conversación desde la hoja de encargo correspondiente.";

    public static final String LEGAL_BINDING_ADVICE_REPLY =
            "No puedo emitir asesoramiento legal vinculante ni indicar una estrategia jurídica definitiva. Puedo ofrecer orientación general y ayudarte a revisar la información disponible en la plataforma.";

    public static final String EMOTIONAL_DISTRESS_REPLY =
            "Entiendo que este mensaje puede reflejar un momento difícil. Soy un asistente conversacional jurídico y no puedo ofrecer apoyo psicológico o de emergencia. Si te sientes desbordado o en riesgo, contacta cuanto antes con una persona de confianza o con un profesional especializado. Si quieres, te ayudo ahora con tu encargo o con un trámite legal en GOA.";

    public static final String UNSUPPORTED_FACTUAL_ASSERTION_REPLY =
            "No puedo confirmar hechos que no estén disponibles en el contexto actual. Puedo ayudarte con orientación general o con la información visible del encargo activo.";

    public static final String AMBIGUOUS_CONTEXT_REPLY =
            "Tu consulta necesita más contexto para responder con seguridad. Si se refiere a un encargo específico, entra en Hojas de Encargo y abre el Asistente de ese encargo.";

    public static final String OUT_OF_DOMAIN_REPLY =
            "Puedo ayudarte con consultas sobre GOA y gestión de encargos. Si quieres, pregúntame por estado, tareas, hitos, documentación o uso de la plataforma.";

    public static final String CLIENT_COURTESY_REPLY =
            "Gracias por tu mensaje. Estoy aquí para ayudarte con cualquier consulta sobre GOA y encargos. Si quieres, continuamos.";

    public static final String PROFESSIONAL_COURTESY_REPLY =
            "Gracias por el mensaje. Sigo disponible para ayudarte con consultas sobre GOA y gestión de encargos.";
}
