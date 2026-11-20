package com.finsight.aiassistant.application;

import com.finsight.aiassistant.api.ChatMessageResponse;
import com.finsight.aiassistant.api.ChatSessionResponse;
import com.finsight.aiassistant.api.CreateChatSessionRequest;
import com.finsight.aiassistant.persistence.ChatMessageEntity;
import com.finsight.aiassistant.persistence.ChatMessageRepository;
import com.finsight.aiassistant.persistence.ChatSessionEntity;
import com.finsight.aiassistant.persistence.ChatSessionRepository;
import com.finsight.common.error.ApiException;
import com.finsight.company.persistence.CompanyEntity;
import com.finsight.company.persistence.CompanyRepository;
import com.finsight.document.persistence.FinancialDocumentEntity;
import com.finsight.document.persistence.FinancialDocumentRepository;
import com.finsight.user.persistence.UserEntity;
import com.finsight.user.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatSessionService {
    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final FinancialDocumentRepository documents;
    private final Clock clock;

    public ChatSessionService(ChatSessionRepository sessions, ChatMessageRepository messages, UserRepository users,
                              CompanyRepository companies, FinancialDocumentRepository documents, Clock clock) {
        this.sessions=sessions; this.messages=messages; this.users=users; this.companies=companies;
        this.documents=documents; this.clock=clock;
    }

    @Transactional
    public ChatSessionResponse create(String userId, CreateChatSessionRequest request) {
        UserEntity user=users.findById(userId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User was not found"));
        CompanyEntity company=empty(request.companyId()) ? null : companies.findById(request.companyId())
                .orElseThrow(() -> notFound("COMPANY_NOT_FOUND", "Company was not found"));
        FinancialDocumentEntity document=empty(request.documentId()) ? null : documents.findByIdAndOwnerId(request.documentId(), userId)
                .filter(doc -> doc.getStatus()==FinancialDocumentEntity.Status.READY)
                .orElseThrow(() -> notFound("DOCUMENT_NOT_FOUND", "Document was not found"));
        String title=empty(request.title()) ? "Financial research chat" : request.title().trim();
        var saved=sessions.save(new ChatSessionEntity(UUID.randomUUID().toString(), user, title, company, document, clock.instant()));
        return toSession(saved);
    }

    @Transactional(readOnly=true)
    public Page<ChatSessionResponse> list(String userId, int page, int size) {
        return sessions.findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(page,size)).map(this::toSession);
    }

    @Transactional(readOnly=true)
    public SessionContext context(String userId, String sessionId) {
        ChatSessionEntity session=owned(userId, sessionId);
        return new SessionContext(session.getId(), session.getTitle(),
                session.getCompany()==null ? null : session.getCompany().getId(),
                session.getDocument()==null ? null : session.getDocument().getId());
    }

    @Transactional(readOnly=true)
    public Page<ChatMessageResponse> messages(String userId, String sessionId, int page, int size) {
        owned(userId, sessionId);
        return messages.findBySessionIdAndSessionUserIdOrderBySequenceNoAsc(sessionId, userId,
                PageRequest.of(page,size)).map(this::toMessage);
    }

    @Transactional
    public void saveExchange(String userId, String sessionId, String question, String answer,
                             List<Map<String,Object>> citations, String model, String promptVersion) {
        ChatSessionEntity session=sessions.findForUpdate(sessionId, userId)
                .orElseThrow(() -> notFound("CHAT_SESSION_NOT_FOUND", "Chat session was not found"));
        long sequence=messages.findLastSequence(sessionId);
        var now=clock.instant();
        messages.save(new ChatMessageEntity(UUID.randomUUID().toString(), session, ++sequence,
                "USER", question, List.of(), null, null, now));
        messages.save(new ChatMessageEntity(UUID.randomUUID().toString(), session, ++sequence,
                "ASSISTANT", answer, citations, model, promptVersion, now));
        session.touched(now);
    }

    private ChatSessionEntity owned(String userId,String sessionId) {
        return sessions.findByIdAndUserId(sessionId,userId)
                .orElseThrow(() -> notFound("CHAT_SESSION_NOT_FOUND", "Chat session was not found"));
    }
    private ChatSessionResponse toSession(ChatSessionEntity session) {
        return new ChatSessionResponse(session.getId(),session.getTitle(),
                session.getCompany()==null ? null : session.getCompany().getId(),
                session.getDocument()==null ? null : session.getDocument().getId(),
                session.getCreatedAt(),session.getUpdatedAt());
    }
    private ChatMessageResponse toMessage(ChatMessageEntity message) {
        return new ChatMessageResponse(message.getId(),message.getSequenceNo(),message.getSpeaker(),
                message.getContent(),message.getCitations(),message.getCreatedAt());
    }
    private boolean empty(String value) { return value==null || value.isBlank(); }
    private ApiException notFound(String code,String message) { return new ApiException(HttpStatus.NOT_FOUND,code,message); }
    public record SessionContext(String id,String title,String companyId,String documentId) { }
}
