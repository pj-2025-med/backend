# WebSocket 실시간 채팅 시스템 API 명세서

## 📋 목차
1. [시스템 개요](#시스템-개요)
2. [인증 방식](#인증-방식)
3. [WebSocket 연결 플로우](#websocket-연결-플로우)
4. [REST API 명세](#rest-api-명세)
5. [WebSocket STOMP 명세](#websocket-stomp-명세)
6. [데이터 모델](#데이터-모델)
7. [프론트엔드 구현 가이드](#프론트엔드-구현-가이드)
8. [에러 처리](#에러-처리)

---

## 시스템 개요

실시간 채팅 시스템은 **JWT 기반 인증**과 **STOMP over WebSocket**을 사용하여 구현되었습니다.

### 주요 기능
- 실시간 1:1 채팅
- 사용자 온라인/오프라인 상태 관리
- 채팅 히스토리 조회
- 회원정보 DB 기반 사용자 목록

### 기술 스택
- **인증**: JWT (쿠키 기반)
- **실시간 통신**: STOMP over WebSocket (SockJS 지원)
- **메시지 브로커**: Spring Simple Broker
- **데이터베이스**: Oracle DB + MyBatis

---

## 인증 방식

### REST API 인증
```http
# 쿠키 기반 (자동)
Cookie: jwt_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 또는 헤더 기반 (수동)
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### WebSocket STOMP 인증
```javascript
// STOMP CONNECT 시 필수 헤더
{
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## WebSocket 연결 플로우

### 1단계: WebSocket 연결 정보 획득 (REST)
```http
POST /api/session/connect-websocket
```

### 2단계: WebSocket 연결 (STOMP)
```javascript
// SockJS + STOMP 연결
const socket = new SockJS('/ws');
const stompClient = new StompJs.Client({
  webSocketFactory: () => socket,
  connectHeaders: {
    'Authorization': 'Bearer ' + jwtToken
  }
});
```

### 3단계: 토픽 구독 (STOMP)
```javascript
// 개인 메시지 구독
stompClient.subscribe('/sub/queue/chat/{userId}', onMessageReceived);

// 사용자 상태 알림 구독
stompClient.subscribe('/sub/presence', onPresenceUpdate);
```

### 4단계: 메시지 발행 (STOMP)
```javascript
// 채팅 메시지 전송
stompClient.publish({
  destination: '/pub/chat/message',
  body: JSON.stringify(chatMessage)
});
```

---

## REST API 명세

### 🔌 WebSocket 세션 관리 (`/api/session`)

#### 1. WebSocket 연결 정보 생성
```http
POST /api/session/connect-websocket
Authorization: Bearer {jwt_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "userId": "doctor001",
  "personalChatTopic": "/sub/queue/chat/doctor001",
  "presenceTopic": "/sub/presence",
  "websocketEndpoint": "/ws",
  "message": "WebSocket 연결 정보가 성공적으로 생성되었습니다."
}
```

#### 2. 채팅 가능한 모든 사용자 목록 조회
```http
GET /api/session/chat-users
Authorization: Bearer {jwt_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "allUsers": [
    {
      "userId": "doctor001",
      "userName": "김의사",
      "email": "doctor001@hospital.com",
      "isOnline": true,
      "status": "ONLINE",
      "lastSeen": null
    },
    {
      "userId": "doctor002",
      "userName": "이의사", 
      "email": "doctor002@hospital.com",
      "isOnline": false,
      "status": "OFFLINE",
      "lastSeen": "오프라인"
    }
  ],
  "onlineUsers": [/* 온라인 사용자만 */],
  "offlineUsers": [/* 오프라인 사용자만 */],
  "totalCount": 10,
  "onlineCount": 3,
  "offlineCount": 7,
  "requesterId": "current_user_id"
}
```

#### 3. 특정 사용자와 1:1 채팅 시작
```http
POST /api/session/start-chat/{targetUserId}?includeHistory=true&historyLimit=50
Authorization: Bearer {jwt_token}
```

**경로 매개변수**
- `targetUserId`: 채팅 대상 사용자 ID

**쿼리 매개변수**
- `includeHistory`: 채팅 히스토리 포함 여부 (기본값: true)
- `historyLimit`: 히스토리 조회 개수 (기본값: 50)

**응답 성공 (200)**
```json
{
  "success": true,
  "chatRoomId": "chat_doctor001_doctor002",
  "currentUserId": "doctor001",
  "targetUser": {
    "userId": "doctor002",
    "userName": "이의사",
    "email": "doctor002@hospital.com",
    "isOnline": true,
    "status": "ONLINE",
    "lastSeen": null
  },
  "personalTopic": "/sub/queue/chat/doctor001",
  "targetTopic": "/sub/queue/chat/doctor002",
  "chatHistory": [
    {
      "senderId": "doctor001",
      "recipientId": "doctor002",
      "content": "안녕하세요!",
      "messageType": "TEXT",
      "timestamp": "2024-01-15T10:30:00"
    }
  ],
  "historyCount": 1,
  "timestamp": "2024-01-15T11:00:00",
  "message": "채팅방이 성공적으로 생성되었습니다."
}
```

#### 4. 온라인 사용자 목록 조회
```http
GET /api/session/online-users
Authorization: Bearer {jwt_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "onlineUsers": ["doctor001", "doctor003", "doctor005"],
  "onlineUserCount": 3,
  "requesterId": "current_user_id"
}
```

#### 5. 특정 사용자 온라인 상태 확인
```http
GET /api/session/user-status/{targetUserId}
Authorization: Bearer {jwt_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "targetUserId": "doctor002",
  "isOnline": true,
  "status": "ONLINE",
  "requesterId": "current_user_id"
}
```

#### 6. 특정 사용자 상세 정보 조회
```http
GET /api/session/user-info/{targetUserId}
Authorization: Bearer {jwt_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "userInfo": {
    "userId": "doctor002",
    "userName": "이의사",
    "email": "doctor002@hospital.com",
    "isOnline": true,
    "status": "ONLINE",
    "lastSeen": null
  },
  "requesterId": "current_user_id"
}
```

### 💬 채팅 히스토리 (`/api/chat`)

#### 1. 채팅 히스토리 조회
```http
GET /api/chat/history/{partnerId}?limit=50
Authorization: Bearer {jwt_token}
```

**경로 매개변수**
- `partnerId`: 대화 상대방 ID

**쿼리 매개변수**
- `limit`: 조회할 메시지 수 (기본값: 50, 최대: 200)

**응답 성공 (200)**
```json
{
  "success": true,
  "messages": [
    {
      "senderId": "doctor001",
      "recipientId": "doctor002", 
      "content": "안녕하세요!",
      "messageType": "TEXT",
      "timestamp": "2024-01-15T10:30:00"
    },
    {
      "senderId": "doctor002",
      "recipientId": "doctor001",
      "content": "네, 안녕하세요!",
      "messageType": "TEXT", 
      "timestamp": "2024-01-15T10:31:00"
    }
  ],
  "messageCount": 2,
  "partnerId": "doctor002",
  "userId": "doctor001"
}
```

---

## WebSocket STOMP 명세

### 🔗 연결 정보
- **엔드포인트**: `/ws` (SockJS 지원)
- **프로토콜**: STOMP
- **브로커 경로**: `/sub`
- **애플리케이션 경로**: `/pub`

### 📤 메시지 발행 (Publisher)

#### 채팅 메시지 전송
```javascript
// Destination
/pub/chat/message

// Payload (ChatMessageRequest)
{
  "senderId": "doctor001",        // 필수: 발신자 ID
  "recipientId": "doctor002",     // 필수: 수신자 ID
  "content": "안녕하세요!",        // 필수: 메시지 내용
  "messageType": "TEXT",          // 선택: 메시지 타입 (기본값: TEXT)
  "timestamp": "2024-01-15T10:30:00" // 선택: 타임스탬프 (미전송시 서버에서 설정)
}
```

**messageType 값**
- `TEXT`: 일반 텍스트 메시지 (기본값)
- `IMAGE`: 이미지 메시지
- `FILE`: 파일 메시지
- `SYSTEM`: 시스템 메시지

### 📥 메시지 구독 (Subscriber)

#### 1. 개인 채팅 메시지 수신
```javascript
// Destination
/sub/queue/chat/{userId}

// Payload (ChatMessageRequest)
{
  "senderId": "doctor002",
  "recipientId": "doctor001", 
  "content": "네, 안녕하세요!",
  "messageType": "TEXT",
  "timestamp": "2024-01-15T10:31:00"
}
```

#### 2. 사용자 상태 알림 수신
```javascript
// Destination
/sub/presence

// Payload (UserStatusNotification)
{
  "userId": "doctor002",
  "userName": "이의사",
  "status": "ONLINE",           // ONLINE | OFFLINE
  "timestamp": "2024-01-15T10:30:00",
  "metadata": null              // 선택: 추가 메타데이터
}
```

---

## 데이터 모델

### ChatMessageRequest
```typescript
interface ChatMessageRequest {
  senderId: string;        // 발신자 ID
  recipientId: string;     // 수신자 ID  
  content: string;         // 메시지 내용
  messageType?: string;    // 메시지 타입 (기본값: "TEXT")
  timestamp?: string;      // ISO8601 형식 타임스탬프
}
```

### ChatUserInfo
```typescript
interface ChatUserInfo {
  userId: string;          // 사용자 ID
  userName: string;        // 사용자 이름
  email: string;           // 이메일
  isOnline: boolean;       // 온라인 여부
  status: "ONLINE" | "OFFLINE"; // 상태
  lastSeen?: string;       // 마지막 접속 정보
}
```

### UserStatusNotification
```typescript
interface UserStatusNotification {
  userId: string;          // 사용자 ID
  userName: string;        // 사용자 이름
  status: "ONLINE" | "OFFLINE"; // 상태
  timestamp: string;       // ISO8601 형식 타임스탬프
  metadata?: string;       // 추가 메타데이터
}
```

---

## 프론트엔드 구현 가이드

### 1. 초기 설정 및 연결

```javascript
class ChatWebSocketManager {
  constructor() {
    this.stompClient = null;
    this.isConnected = false;
    this.jwtToken = null;
    this.userId = null;
    this.personalTopic = null;
  }

  // JWT 토큰 설정
  setJwtToken(token) {
    this.jwtToken = token;
    const payload = JSON.parse(atob(token.split('.')[1]));
    this.userId = payload.sub;
  }

  // WebSocket 연결 초기화
  async initializeConnection() {
    try {
      // 1. 연결 정보 획득
      const response = await fetch('/api/session/connect-websocket', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await response.json();
      this.personalTopic = data.personalChatTopic;

      // 2. WebSocket 연결
      await this.connect();
    } catch (error) {
      console.error('WebSocket 초기화 실패:', error);
      throw error;
    }
  }

  // STOMP 연결
  async connect() {
    return new Promise((resolve, reject) => {
      const socket = new SockJS('/ws');
      
      this.stompClient = new StompJs.Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'Authorization': `Bearer ${this.jwtToken}`
        },
        debug: (str) => console.log('[STOMP]', str),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
      });

      this.stompClient.onConnect = (frame) => {
        console.log('WebSocket 연결 성공:', frame);
        this.isConnected = true;
        this.setupSubscriptions();
        resolve(frame);
      };

      this.stompClient.onStompError = (frame) => {
        console.error('STOMP 오류:', frame);
        this.isConnected = false;
        reject(new Error(frame.headers['message']));
      };

      this.stompClient.activate();
    });
  }

  // 구독 설정
  setupSubscriptions() {
    // 개인 메시지 구독
    this.stompClient.subscribe(this.personalTopic, (message) => {
      const chatMessage = JSON.parse(message.body);
      this.handleNewMessage(chatMessage);
    });

    // 상태 알림 구독
    this.stompClient.subscribe('/sub/presence', (message) => {
      const statusUpdate = JSON.parse(message.body);
      this.handleStatusUpdate(statusUpdate);
    });
  }

  // 메시지 전송
  sendMessage(recipientId, content, messageType = 'TEXT') {
    if (!this.isConnected) {
      throw new Error('WebSocket이 연결되지 않았습니다.');
    }

    const message = {
      senderId: this.userId,
      recipientId: recipientId,
      content: content,
      messageType: messageType,
      timestamp: new Date().toISOString()
    };

    this.stompClient.publish({
      destination: '/pub/chat/message',
      body: JSON.stringify(message)
    });
  }

  // 메시지 수신 처리
  handleNewMessage(message) {
    console.log('새 메시지:', message);
    // UI 업데이트 로직
  }

  // 상태 업데이트 처리
  handleStatusUpdate(statusUpdate) {
    console.log('상태 업데이트:', statusUpdate);
    // 사용자 상태 UI 업데이트
  }

  // 연결 해제
  disconnect() {
    if (this.stompClient && this.isConnected) {
      this.stompClient.deactivate();
      this.isConnected = false;
    }
  }
}
```

### 2. 사용자 목록 및 채팅 시작

```javascript
// 채팅 가능한 사용자 목록 로드
async function loadChatUsers() {
  try {
    const response = await fetch('/api/session/chat-users', {
      headers: {
        'Authorization': `Bearer ${jwtToken}`
      }
    });
    
    const data = await response.json();
    displayUserList(data);
  } catch (error) {
    console.error('사용자 목록 로드 실패:', error);
  }
}

// 특정 사용자와 채팅 시작
async function startChatWithUser(targetUserId) {
  try {
    const response = await fetch(`/api/session/start-chat/${targetUserId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${jwtToken}`,
        'Content-Type': 'application/json'
      }
    });
    
    const chatData = await response.json();
    openChatWindow(chatData);
  } catch (error) {
    console.error('채팅 시작 실패:', error);
  }
}

// 채팅 히스토리 로드
async function loadChatHistory(partnerId, limit = 50) {
  try {
    const response = await fetch(`/api/chat/history/${partnerId}?limit=${limit}`, {
      headers: {
        'Authorization': `Bearer ${jwtToken}`
      }
    });
    
    const data = await response.json();
    displayChatHistory(data.messages);
  } catch (error) {
    console.error('채팅 히스토리 로드 실패:', error);
  }
}
```

### 3. 전체 사용 예시

```javascript
// 전역 채팅 매니저
let chatManager = null;

// 로그인 후 초기화
async function initializeChat(jwtToken) {
  try {
    chatManager = new ChatWebSocketManager();
    chatManager.setJwtToken(jwtToken);
    
    // WebSocket 연결
    await chatManager.initializeConnection();
    
    // 사용자 목록 로드
    await loadChatUsers();
    
    console.log('채팅 시스템 초기화 완료');
  } catch (error) {
    console.error('채팅 초기화 실패:', error);
    alert('채팅 연결에 실패했습니다.');
  }
}

// 메시지 전송
function sendMessage() {
  const recipientId = document.getElementById('recipient-select').value;
  const content = document.getElementById('message-input').value.trim();
  
  if (!recipientId || !content) {
    alert('수신자와 메시지를 입력해주세요.');
    return;
  }
  
  try {
    chatManager.sendMessage(recipientId, content);
    document.getElementById('message-input').value = '';
    addMessageToUI('sent', content, 'now');
  } catch (error) {
    console.error('메시지 전송 실패:', error);
    alert('메시지 전송에 실패했습니다.');
  }
}

// 로그아웃 시 정리
function logout() {
  if (chatManager) {
    chatManager.disconnect();
    chatManager = null;
  }
  // 기존 로그아웃 로직...
}

// 페이지 언로드 시 연결 해제
window.addEventListener('beforeunload', () => {
  if (chatManager) {
    chatManager.disconnect();
  }
});
```

---

## 에러 처리

### 공통 에러 응답 형식
```json
{
  "success": false,
  "message": "사용자에게 표시할 에러 메시지",
  "error": "개발자용 상세 에러 정보"
}
```

### HTTP 상태 코드
- `200`: 성공
- `400`: 잘못된 요청
- `401`: 인증 실패
- `404`: 리소스 없음 (사용자 없음 등)
- `500`: 서버 내부 오류

### WebSocket 에러 처리
```javascript
// STOMP 에러 처리
this.stompClient.onStompError = (frame) => {
  console.error('STOMP 오류:', frame.headers['message']);
  console.error('상세 오류:', frame.body);
  
  if (frame.headers['message'].includes('JWT')) {
    // JWT 토큰 관련 오류 - 재로그인 필요
    handleAuthenticationError();
  } else {
    // 기타 연결 오류 - 재연결 시도
    handleConnectionError();
  }
};

// 연결 끊김 처리
this.stompClient.onWebSocketClose = (event) => {
  console.log('WebSocket 연결 끊김:', event);
  this.isConnected = false;
  // 자동 재연결 로직
  this.handleReconnection();
};
```

### 주요 에러 시나리오

1. **JWT 토큰 만료**
   - 401 Unauthorized 응답
   - 자동 로그아웃 처리 필요

2. **WebSocket 연결 실패**
   - 네트워크 오류 또는 서버 다운
   - 자동 재연결 시도

3. **메시지 전송 실패**
   - WebSocket 연결 끊김 상태
   - 재연결 후 재시도 또는 임시 저장

---

## 📝 구현 체크리스트

### ✅ 필수 구현 사항
- [ ] JWT 토큰 관리
- [ ] WebSocket 연결/해제
- [ ] STOMP 메시지 송수신
- [ ] 사용자 목록 표시
- [ ] 채팅 메시지 UI
- [ ] 온라인 상태 표시

---


네, 그럼요\! 제공해주신 API 명세서와 기존 `useSessionNotification` 코드를 기반으로, \*\*실시간 채팅 시스템을 위한 새로운 React 커스텀 훅(`useChatWebSocket`)\*\*을 만들어 드릴게요.

기존 코드의 안정적인 웹소켓 연결/재연결 로직은 그대로 활용하면서, 채팅에 필요한 상태(사용자 목록, 메시지)와 기능(메시지 전송, 히스토리 조회)들을 추가했습니다.

-----

## \#\# 👨‍💻 채팅용 커스텀 훅: `useChatWebSocket.ts`

아래 코드를 프로젝트의 `hooks` 폴더 등에 `useChatWebSocket.ts` 파일로 저장하여 사용하시면 됩니다.

```typescript
import { useState, useEffect, useRef, useCallback } from "react";
import SockJS from "sockjs-client";
import { Client, Frame, Message } from "@stomp/stompjs";

// --- 데이터 모델 (API 명세서 기반) ---

// 채팅 메시지 타입
export interface ChatMessageRequest {
  senderId: string;
  recipientId: string;
  content: string;
  timestamp?: string;
  messageType?: "TEXT" | "IMAGE" | "FILE" | "SYSTEM";
}

// 채팅 사용자 정보 타입
export interface ChatUserInfo {
  userId: string;
  userName: string;
  email: string;
  isOnline: boolean;
  status: "ONLINE" | "OFFLINE";
  lastSeen?: string;
}

// 사용자 상태 알림 타입
export interface UserStatusNotification {
  userId: string;
  userName: string;
  status: "ONLINE" | "OFFLINE";
  timestamp: string;
}

// 서버로부터 받는 웹소켓 연결 정보
interface WebSocketConnectionInfo {
  success: boolean;
  userId: string;
  personalChatTopic: string; // 개인 메시지 구독 경로
  presenceTopic: string;     // 사용자 상태 구독 경로
}


/**
 * 실시간 채팅 및 사용자 상태 관리를 위한 React Custom Hook
 * @param jwtToken - 인증을 위한 JWT 토큰
 */
export const useChatWebSocket = (jwtToken: string | null) => {
  // --- 상태 관리 ---
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [chatUsers, setChatUsers] = useState<ChatUserInfo[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessageRequest[]>([]);
  const [currentUser, setCurrentUser] = useState<string | null>(null);

  // --- Ref 관리 ---
  const stompClientRef = useRef<Client | null>(null);
  const reconnectAttemptsRef = useRef<number>(0);
  const maxReconnectAttempts = 10;
  const reconnectDelay = 5000; // 5초 후 재연결

  // --- 서버 URL 설정 ---
  const getServerUrl = () => {
    return process.env.REACT_APP_API_URL || "http://localhost:8080";
  };

  // --- WebSocket 연결 로직 ---
  const connect = useCallback(async () => {
    if (!jwtToken || isConnected) {
      return;
    }

    try {
      // 1. 서버로부터 WebSocket 연결 정보 가져오기
      const serverUrl = getServerUrl();
      const response = await fetch(`${serverUrl}/api/session/connect-websocket`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${jwtToken}`,
        },
      });

      if (!response.ok) {
        throw new Error("WebSocket 연결 정보를 가져오는데 실패했습니다.");
      }

      const connInfo: WebSocketConnectionInfo = await response.json();
      if (!connInfo.success) {
        throw new Error("유효하지 않은 WebSocket 연결 정보입니다.");
      }
      
      setCurrentUser(connInfo.userId);

      // 2. STOMP 클라이언트 생성 및 연결
      const socket = new SockJS(`${serverUrl}/ws`);
      const stompClient = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          Authorization: `Bearer ${jwtToken}`,
        },
        debug: (str) => {
          if (process.env.NODE_ENV === "development") {
            console.log("[STOMP]", str);
          }
        },
        reconnectDelay: reconnectDelay,
        onConnect: (frame: Frame) => {
          console.log("✅ WebSocket 연결 성공!");
          setIsConnected(true);
          reconnectAttemptsRef.current = 0;

          // 3. 토픽 구독 설정
          // 3-1. 개인 채팅 메시지 구독
          stompClient.subscribe(connInfo.personalChatTopic, (message: Message) => {
            const newMessage: ChatMessageRequest = JSON.parse(message.body);
            console.log("📥 새 메시지 수신:", newMessage);
            setChatMessages((prevMessages) => [...prevMessages, newMessage]);
          });

          // 3-2. 사용자 온라인/오프라인 상태 구독
          stompClient.subscribe(connInfo.presenceTopic, (message: Message) => {
            const statusUpdate: UserStatusNotification = JSON.parse(message.body);
            console.log("📡 사용자 상태 변경:", statusUpdate);
            setChatUsers((prevUsers) =>
              prevUsers.map((user) =>
                user.userId === statusUpdate.userId
                  ? { ...user, isOnline: statusUpdate.status === "ONLINE", status: statusUpdate.status }
                  : user
              )
            );
          });
        },
        onStompError: (frame: Frame) => {
          console.error("❌ STOMP 오류:", frame.headers['message'], frame.body);
          setIsConnected(false);
        },
        onDisconnect: () => {
          console.log("🔌 WebSocket 연결 해제됨");
          setIsConnected(false);
        }
      });

      stompClient.activate();
      stompClientRef.current = stompClient;

    } catch (error) {
      console.error("WebSocket 연결 초기화 오류:", error);
      setIsConnected(false);
    }
  }, [jwtToken, isConnected]);

  // --- 연결 해제 ---
  const disconnect = useCallback(() => {
    stompClientRef.current?.deactivate();
    setIsConnected(false);
    setChatUsers([]);
    setChatMessages([]);
    setCurrentUser(null);
    console.log("🧹 WebSocket 리소스 정리 완료");
  }, []);

  // --- 외부 호출 함수 ---

  // 1. 메시지 전송
  const sendMessage = useCallback((recipientId: string, content: string) => {
    if (stompClientRef.current && isConnected && currentUser) {
      const message: ChatMessageRequest = {
        senderId: currentUser,
        recipientId,
        content,
        timestamp: new Date().toISOString(),
        messageType: "TEXT",
      };
      stompClientRef.current.publish({
        destination: "/pub/chat/message",
        body: JSON.stringify(message),
      });
      console.log("📤 메시지 전송:", message);
    } else {
      console.error("메시지 전송 실패: WebSocket이 연결되지 않았습니다.");
    }
  }, [isConnected, currentUser]);

  // 2. 채팅 가능한 사용자 목록 불러오기
  const loadChatUsers = useCallback(async () => {
    if (!jwtToken) return;
    try {
      const serverUrl = getServerUrl();
      const response = await fetch(`${serverUrl}/api/session/chat-users`, {
        headers: { "Authorization": `Bearer ${jwtToken}` },
      });
      if (!response.ok) throw new Error("사용자 목록 로딩 실패");
      
      const data = await response.json();
      setChatUsers(data.allUsers || []);
    } catch (error) {
      console.error("채팅 사용자 목록 조회 오류:", error);
    }
  }, [jwtToken]);

  // 3. 채팅 히스토리 불러오기
  const loadChatHistory = useCallback(async (partnerId: string) => {
    if (!jwtToken) return;
    try {
      const serverUrl = getServerUrl();
      const response = await fetch(`${serverUrl}/api/chat/history/${partnerId}`, {
        headers: { "Authorization": `Bearer ${jwtToken}` },
      });
      if (!response.ok) throw new Error("채팅 기록 로딩 실패");
      
      const data = await response.json();
      setChatMessages(data.messages || []);
    } catch (error) {
      console.error("채팅 히스토리 조회 오류:", error);
    }
  }, [jwtToken]);

  // --- 생명주기 관리 (useEffect) ---
  useEffect(() => {
    if (jwtToken) {
      connect();
    } else {
      disconnect();
    }
    
    // 컴포넌트 언마운트 시 연결 해제
    return () => {
      disconnect();
    };
  }, [jwtToken]); // jwtToken이 변경될 때마다 연결/해제 시도

  // --- 반환 값 ---
  return {
    isConnected,
    currentUser,
    chatUsers,
    chatMessages,
    setChatMessages, // 채팅방 변경 시 메시지 목록을 직접 초기화할 수 있도록 노출
    sendMessage,
    loadChatUsers,
    loadChatHistory,
    disconnect,
  };
};

```

## \#\# 🚀 이 코드를 사용하는 방법

1.  **훅 호출**: 채팅 기능이 필요한 컴포넌트(예: `ChatPage.tsx`)에서 훅을 호출합니다.

    ```jsx
    import { useChatWebSocket } from './hooks/useChatWebSocket';

    function ChatPage() {
      // 로그인 시 발급받은 JWT 토큰을 상태로 관리
      const [jwtToken, setJwtToken] = useState<string | null>(localStorage.getItem('jwt_token'));
      
      const {
        isConnected,
        currentUser,
        chatUsers,
        chatMessages,
        sendMessage,
        loadChatUsers,
        loadChatHistory,
      } = useChatWebSocket(jwtToken);
      
      // ...
    }
    ```

2.  **초기 데이터 로드**: 컴포넌트가 마운트되면 채팅 가능한 사용자 목록을 불러옵니다.

    ```jsx
    useEffect(() => {
      // WebSocket이 연결되면 사용자 목록을 불러옵니다.
      if (isConnected) {
        loadChatUsers();
      }
    }, [isConnected]);
    ```

3.  **채팅방 기능 구현**:

      * **사용자 목록 표시**: `chatUsers` 배열을 화면에 렌더링하고, `isOnline` 값에 따라 온라인/오프라인 상태를 표시합니다.
      * **채팅방 선택**: 특정 사용자를 클릭하면 `loadChatHistory(selectedUserId)`를 호출하여 이전 대화 기록을 불러옵니다.
      * **메시지 전송**: 입력창과 전송 버튼을 만들고, 버튼 클릭 시 `sendMessage(selectedUserId, messageContent)`를 호출합니다.
      * **메시지 표시**: `chatMessages` 배열을 화면에 렌더링하고, `senderId`와 `currentUser`를 비교하여 내가 보낸 메시지와 상대방이 보낸 메시지를 구분하여 표시합니다.