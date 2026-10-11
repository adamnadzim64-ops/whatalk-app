# WhaTalk Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **Authentication Gate**: Unauthenticated requests (`request.auth == null`) are denied across all collections.
2. **PII Isolation**: Public user profiles (`users/{userId}`) never contain raw phone numbers or emails. Private account metadata and preferences are isolated in `userSettings/{userId}` where only `request.auth.uid == userId` has read/write access.
3. **Conversation & Message Membership**: A conversation (`conversations/{conversationId}`) and its messages (`messages/{messageId}`) can only be read or listed by users whose UID is present in `resource.data.memberUids`.
4. **Sender Ownership**: A message (`messages/{messageId}`) can only be created if `incoming().senderId == request.auth.uid` and `request.auth.uid in incoming().memberUids`. Content edits (`text`, `isEdited`, `isDeleted`) are restricted to the original `senderId`.
5. **Contact & Block Ownership**: `contacts/{contactId}` and `blockedUsers/{blockId}` documents are strictly scoped to `ownerId == request.auth.uid`.
6. **Status Visibility**: `statuses/{statusId}` can only be read/listed by users in `visibleToUids` or the `authorId`.
7. **Call Signaling Privacy**: `calls/{callId}` can only be accessed by participants in `participantUids`.
8. **Immutable Creation Metadata**: `createdAt` and primary owner/author IDs (`userId`, `ownerId`, `senderId`, `creatorId`, `authorId`, `callerId`, `reporterId`) cannot be mutated on `update`.
9. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be valid server timestamps (`<= request.time`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Scrape**: `unauthDb.collection("messages").get()` -> Must fail (`PERMISSION_DENIED`).
2. **Cross-User Settings Read (PII Leak)**: Alice reads `userSettings/bob_uid` -> Must fail.
3. **Identity Spoofing on Message Create**: Alice creates a message with `senderId: "bob_uid"` -> Must fail.
4. **Non-Member Message Read**: Eve reads a message where `memberUids` is `["alice_uid", "bob_uid"]` -> Must fail.
5. **Shadow Field Injection**: Alice creates a user profile with an undeclared field `isAdmin: true` -> Must fail (`hasOnly` check).
6. **Value Poisoning (Oversized String)**: Alice sends a message with `text` length > 4000 characters -> Must fail.
7. **ID Poisoning**: Creating a document with an invalid ID containing spaces or special characters -> Must fail (`isValidId`).
8. **Immortal Field Mutation**: Alice updates `createdAt` or `senderId` on an existing message -> Must fail.
9. **Unauthorized Message Content Edit**: Bob attempts to change `text` on Alice's message -> Must fail (Bob can only update `status` or `reactionsSummary`).
10. **Unscoped List Query (Query Trust Test)**: Alice queries `conversations` without `.whereArrayContains("memberUids", aliceUid)` -> Must fail.
11. **Cross-User Contact Deletion**: Bob tries to delete Alice's contact document -> Must fail.
12. **Report Tampering**: Reporter attempts to mutate or delete a submitted abuse report in `reports/{reportId}` -> Must fail.
