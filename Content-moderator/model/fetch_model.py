"""Run at worker image build time to bake model weights into the image.

Downloads the pre-quantized ONNX export + tokenizer files for the toxicity
model, pinned to a specific revision so builds are reproducible.
"""
import os
import sys

from huggingface_hub import snapshot_download

REPO_ID = os.environ.get("HF_MODEL_REPO", "minuva/MiniLMv2-toxic-jigaw-lite-onnx")
REVISION = os.environ.get("HF_MODEL_REVISION")
TARGET_DIR = os.environ.get("MODEL_DIR", "/model")


def main() -> None:
    if not REVISION:
        print("HF_MODEL_REVISION must be set for a reproducible build", file=sys.stderr)
        sys.exit(1)

    path = snapshot_download(
        repo_id=REPO_ID,
        revision=REVISION,
        local_dir=TARGET_DIR,
        allow_patterns=[
            "*.onnx",
            "tokenizer.json",
            "tokenizer_config.json",
            "vocab.txt",
            "special_tokens_map.json",
            "config.json",
        ],
    )
    print(f"Model fetched to {path}")


if __name__ == "__main__":
    main()
