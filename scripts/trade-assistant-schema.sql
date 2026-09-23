CREATE TABLE IF NOT EXISTS trade_assistant_conversations (
    candidate_id BIGINT NOT NULL PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS trade_assistant_messages (
    candidate_id BIGINT NOT NULL,
    message_order INTEGER NOT NULL,
    role VARCHAR(16) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    PRIMARY KEY (candidate_id, message_order),
    FOREIGN KEY (candidate_id) REFERENCES trade_assistant_conversations(candidate_id)
);
