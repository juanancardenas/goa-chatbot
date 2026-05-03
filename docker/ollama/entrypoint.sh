#!/usr/bin/env bash
set -e

MODEL="${OLLAMA_MODEL:-llama3.2:3b}"

ollama serve &
OLLAMA_PID=$!

echo "Waiting for Ollama server..."
sleep 5

echo "Checking Ollama model: ${MODEL}"
if ! ollama list | awk '{print $1}' | grep -Fxq "${MODEL}"; then
  echo "Pulling Ollama model: ${MODEL}"
  ollama pull "${MODEL}"
else
  echo "Ollama model already available: ${MODEL}"
fi

wait "${OLLAMA_PID}"