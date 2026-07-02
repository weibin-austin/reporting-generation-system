#!/usr/bin/env python3
"""Local stand-in for lambda/sendEmailCode.py.

Drains email_queue on LocalStack and delivers each notification through the
macOS Mail.app default account (no SMTP credentials needed). Run it after
triggering a report while the stack from README section 1 is up:

    python3 scripts/local_email_worker.py
"""
import json
import subprocess
import time

QUEUE_URL = "http://localhost:4566/000000000000/email_queue"
LOCALSTACK_CONTAINER = "reporting-localstack"
EXPECTED_TOKEN = "12345"  # same shared token the Lambda checks
DRAIN_SECONDS = 60  # keep polling this long so in-flight messages reappear


def awslocal(*args):
    out = subprocess.run(
        ["docker", "exec", LOCALSTACK_CONTAINER, "awslocal", *args],
        capture_output=True, text=True, check=True,
    ).stdout
    return json.loads(out) if out.strip() else {}


def applescript_quote(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def send_via_mail_app(to, subject, body):
    script = f"""
    tell application "Mail"
        set newMessage to make new outgoing message with properties {{subject:{applescript_quote(subject)}, content:{applescript_quote(body)}, visible:false}}
        tell newMessage
            make new to recipient at end of to recipients with properties {{address:{applescript_quote(to)}}}
        end tell
        send newMessage
    end tell
    """
    subprocess.run(["osascript", "-e", script], check=True, capture_output=True, text=True)


def main():
    sent = 0
    deadline = time.time() + DRAIN_SECONDS
    while time.time() < deadline:
        resp = awslocal("sqs", "receive-message", "--queue-url", QUEUE_URL,
                        "--max-number-of-messages", "10", "--wait-time-seconds", "5",
                        "--output", "json")
        messages = resp.get("Messages", [])
        if not messages and sent:
            break  # queue drained
        for msg in messages:
            payload = json.loads(msg["Body"])
            if payload.get("token") != EXPECTED_TOKEN:
                print(f"skipping message with bad token: {payload}")
                continue
            print(f"sending to {payload['to']}: {payload['subject']!r} / {payload['body']!r}")
            send_via_mail_app(payload["to"], payload["subject"], payload["body"])
            awslocal("sqs", "delete-message", "--queue-url", QUEUE_URL,
                     "--receipt-handle", msg["ReceiptHandle"])
            sent += 1
    print(f"done, {sent} email(s) sent")


if __name__ == "__main__":
    main()
