#!/usr/bin/env bash
set -euo pipefail

# Before making changes to this file, please read doc/install-sh.md for context and guidelines.

# Amp CLI Installation Script
# Installs Bun and runs JavaScript bootstrap script

# Configuration
AMP_HOME="${AMP_HOME:-$HOME/.amp}"
BIN_DIR="$AMP_HOME/bin"
STORAGE_BASE="https://storage.googleapis.com/amp-public-assets-prod-0"
AMP_URL="${AMP_URL:-https://ampcode.com}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Cleanup on interrupt
cleanup() {
  echo -e "\n${YELLOW}The spell has been interrupted... Dispersing magical energies...${NC}"
  rm "$AMP_HOME/amp-install-*" 2>/dev/null || true
  exit 1
}

trap cleanup INT TERM

log() {
  echo -e "${BLUE}[INFO]${NC} $1" >&2
}

warn() {
  echo -e "${YELLOW}[WARN]${NC} $1" >&2
}

error() {
  echo -e "${RED}[ERROR]${NC} $1" >&2
  exit 1
}

success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1" >&2
}

# Check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Require command to exist or exit with error
need_cmd() {
  if ! command_exists "$1"; then
    error "need '$1' (command not found)"
  fi
}

# Check all prerequisite commands upfront
check_prereqs() {
  for cmd in uname mktemp chmod mkdir rm tar find grep; do
    need_cmd "$cmd"
  done
}

# Detect target platform for Bun binary
# Adapted from https://bun.sh/install.sh (retrieved September 1st, 2025)
detect_bun_target() {
  local platform
  platform="$(uname -s) $(uname -m)"

  case $platform in
    'Darwin x86_64')
      target=darwin-x64
      ;;
    'Darwin arm64')
      target=darwin-aarch64
      ;;
    'Linux aarch64' | 'Linux arm64')
      target=linux-aarch64
      ;;
    'MINGW64'*)
      target=windows-x64
      ;;
    'Linux riscv64')
      error 'Not supported on riscv64'
      ;;
    'Linux x86_64' | *)
      target=linux-x64
      ;;
  esac

  case "$target" in
    'linux'*)
      if [ -f /etc/alpine-release ]; then
        target="$target-musl"
      fi
      ;;
  esac

  if [[ $target = darwin-x64 ]]; then
    # Is this process running in Rosetta?
    # redirect stderr to devnull to avoid error message when not running in Rosetta
    if [[ $(sysctl -n sysctl.proc_translated 2>/dev/null) = 1 ]]; then
      target=darwin-aarch64
      log "Your shell is running in Rosetta 2. Downloading bun for $target instead"
    fi
  fi

  # If AVX2 isn't supported, use the -baseline build
  case "$target" in
    'darwin-x64'*)
      if [[ $(sysctl -a | grep machdep.cpu | grep AVX2) == '' ]]; then
        target="$target-baseline"
      fi
      ;;
    'linux-x64'*)
      # If AVX2 isn't supported, use the -baseline build
      if [[ $(grep avx2 /proc/cpuinfo) = '' ]]; then
        target="$target-baseline"
      fi
      ;;
  esac

  echo "$target"
}

# Robust downloader that handles snap curl issues
downloader() {
  local url="$1"
  local output_file="$2"

  # Check if we have a broken snap curl
  # https://github.com/boukendesho/curl-snap/issues/1
  local snap_curl=0
  if command_exists curl; then
    local curl_path
    curl_path=$(command -v curl)
    if [[ "$curl_path" == *"/snap/"* ]]; then
      snap_curl=1
    fi
  fi

  # Check if we have a working (non-snap) curl
  if command_exists curl && [[ $snap_curl -eq 0 ]]; then
    curl -fsSL "$url" -o "$output_file"
  # Try wget for both no curl and the broken snap curl
  elif command_exists wget; then
    wget -q --show-progress "$url" -O "$output_file"
  # If we can't fall back from broken snap curl to wget, report the broken snap curl
  elif [[ $snap_curl -eq 1 ]]; then
    error "curl installed with snap cannot download files due to missing permissions. Please uninstall it and reinstall curl with a different package manager (e.g., apt). See https://github.com/boukendesho/curl-snap/issues/1"
  else
    error "Neither curl nor wget found. Please install one of them."
  fi
}

# Download file with progress
download_file() {
  local url="$1"
  local output_file="$2"

  log "Channeling $(basename "$output_file") from the ethereal realm..."

  # Add dry-run mode for testing
  if [[ "${DRY_RUN:-}" == "1" ]]; then
    log "[DRY RUN] Would download: $url -> $output_file"
    touch "$output_file"
    return
  fi

  # Use secure temporary file
  local temp_file
  temp_file=$(mktemp "$(dirname "$output_file")/tmp.XXXXXX")

  # Download to temp file first, then atomic move
  downloader "$url" "$temp_file"
  mv "$temp_file" "$output_file"
}

# Extract archive
extract_archive() {
  local archive="$1"
  local extract_dir="$2"
  local binary_name="$3"

  # Add dry-run mode for testing
  if [[ "${DRY_RUN:-}" == "1" ]]; then
    log "[DRY RUN] Would extract: $archive -> find $binary_name -> $BIN_DIR/$binary_name"
    touch "$BIN_DIR/$binary_name"
    chmod +x "$BIN_DIR/$binary_name"
    return
  fi

  # Extract without stripping first
  case "$archive" in
    *.tar.gz | *.tgz)
      tar -xzf "$archive" -C "$extract_dir"
      ;;
    *.zip)
      if command_exists unzip; then
        unzip -q "$archive" -d "$extract_dir"
      else
        error "unzip not found. Please install unzip to extract .zip files."
      fi
      ;;
    *)
      error "Unsupported archive format: $archive"
      ;;
  esac

  # Find and move the binary to the correct location
  local binary_path
  binary_path=$(find "$extract_dir" -type f -name "$binary_name" | head -1)

  # If not found, try again stripping the first path component (archives
  # that are wrapped in a single top-level directory, e.g. Bun)
  if [[ -z "$binary_path" && "$archive" == *.tar.gz* ]]; then
    tar -xzf "$archive" -C "$extract_dir" --strip-components=1
    binary_path=$(find "$extract_dir" -type f -name "$binary_name" | head -1)
  fi

  if [[ -n "$binary_path" ]]; then
    mv "$binary_path" "$BIN_DIR/$binary_name"
    chmod +x "$BIN_DIR/$binary_name"
  else
    error "Binary $binary_name not found in extracted archive"
  fi
}

# Install Bun
install_bun() {
  local bun_target="$1"
  local bun_binary="bun"

  # Check for unsupported musl platforms
  if [[ "$bun_target" == *"-musl"* ]]; then
    error "musl-based Linux distributions (like Alpine) are not currently supported. Please use a glibc-based distribution."
  fi

  local bun_archive="bun-${bun_target}.tar.gz"
  local bun_url="$STORAGE_BASE/bun/$bun_archive"
  local bun_path="$BIN_DIR/$bun_archive"

  if [[ -f "$BIN_DIR/$bun_binary" ]]; then
    return
  fi

  download_file "$bun_url" "$bun_path"

  local temp_dir
  temp_dir=$(mktemp -d)
  extract_archive "$bun_path" "$temp_dir" "$bun_binary"
  rm -rf "$temp_dir" "$bun_path"

  success "Bun's mystical powers have been awakened"
}

# Download and run bootstrap script
run_bootstrap() {
  local bootstrap_url="$AMP_URL/bootstrap.ts"
  local bootstrap_path="$AMP_HOME/amp-bootstrap-$$.ts"

  # Run from AMP_HOME to prevent bun trying to read configs in CWD
  cd "$AMP_HOME"

  download_file "$bootstrap_url" "$bootstrap_path"
  "$BIN_DIR/bun" run --no-install "$bootstrap_path"

  # Cleanup
  rm -f "$bootstrap_path"
}

# Main installation function
main() {
  log "Summoning the Amp CLI wizard..."

  # Check prerequisites first
  check_prereqs

  # Create directories
  mkdir -p "$BIN_DIR"

  # Detect platform
  local bun_target
  bun_target=$(detect_bun_target)

  # Install Bun
  install_bun "$bun_target"

  # Run bootstrap script to handle CLI installation
  run_bootstrap
}

# Run main function
main "$@"
