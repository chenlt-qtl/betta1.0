#!/usr/bin/env python3
"""校验并导入牛津高阶第 10 版词典数据。"""

from __future__ import annotations

import argparse
import dataclasses
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import unicodedata
from collections import defaultdict
from collections.abc import Iterable, Iterator, Sequence

SOURCE = "oald10"
PROFILE_PREFIX = "/profile/dictionary/oald10"
DEFAULT_SOURCE_DIR = Path("/Users/unagi/Documents/资料/牛津高阶英汉双解词典第10版数据包/export")
DEFAULT_AUDIO_DESTINATION = Path("/Users/unagi/Documents/file/betta/upload/dictionary/oald10")


@dataclasses.dataclass(frozen=True)
class WordRow:
    word_name: str
    display_name: str
    phonetics: str | None
    phonetics_uk: str | None
    phonetics_us: str | None
    ph_mp3: str | None
    ph_mp3_uk: str | None
    ph_mp3_us: str | None
    acceptation: str | None
    exchange: str | None
    parts: str | None
    word_forms: str | None
    usage_labels: str | None
    variants: str | None
    grammar: str | None
    dictionary_detail: str


@dataclasses.dataclass(frozen=True)
class SentenceRow:
    word_name: str
    sense_id: str | None
    sort_order: int
    orig: str
    trans: str | None
    audio_path: str | None


@dataclasses.dataclass(frozen=True)
class AliasRow:
    alias_key: str
    raw_alias_key: str
    raw_target_key: str
    target_key: str
    sort_order: int


@dataclasses.dataclass(frozen=True)
class ImportData:
    words: tuple[WordRow, ...]
    sentences: tuple[SentenceRow, ...]
    aliases: tuple[AliasRow, ...]
    sense_count: int
    source_alias_relation_count: int
    referenced_audio: tuple[Path, ...]
    missing_audio: tuple[Path, ...]


def normalize_key(value: str) -> str:
    """按应用查询规则规范化词头和别名。"""
    return unicodedata.normalize("NFKC", value or "").strip().lower()


def strip_phonetic(value: object) -> str | None:
    text = text_value(value)
    if text and len(text) >= 2 and text.startswith("/") and text.endswith("/"):
        return text[1:-1].strip() or None
    return text


def text_value(value: object) -> str | None:
    if value is None:
        return None
    if isinstance(value, str):
        return value.strip() or None
    if isinstance(value, Sequence) and not isinstance(value, (bytes, bytearray)):
        text = "; ".join(str(item).strip() for item in value if str(item).strip())
        return text or None
    text = str(value).strip()
    return text or None


def profile_audio_path(relative_path: object) -> str | None:
    text = text_value(relative_path)
    if not text:
        return None
    return f"{PROFILE_PREFIX}/{Path(text).as_posix()}"


def format_acceptation(senses: Sequence[dict[str, object]]) -> str | None:
    """仅将中文义项转为现有页面使用的 ASCII 竖线分隔格式。"""
    items: list[str] = []
    for sense in senses:
        definition = sense.get("definition") or {}
        if not isinstance(definition, dict):
            continue
        zh = text_value(definition.get("zh"))
        if not zh:
            continue
        pos = text_value(sense.get("pos"))
        items.append(f"{pos} {zh}" if pos else zh)
    return "|".join(items) or None


def unique_parts(senses: Sequence[dict[str, object]]) -> str | None:
    parts: list[str] = []
    for sense in senses:
        part = text_value(sense.get("pos"))
        if part and part not in parts:
            parts.append(part)
    return "; ".join(parts) or None


def to_word_row(entry: dict[str, object]) -> WordRow:
    senses = entry.get("senses") or []
    if not isinstance(senses, list):
        raise ValueError(f"词条 {entry.get('lookup_key')!r} 的 senses 不是数组")
    word_name = normalize_key(str(entry.get("lookup_key") or ""))
    if not word_name:
        raise ValueError("发现空 lookup_key")
    uk = text_value(entry.get("uk"))
    us = text_value(entry.get("us"))
    uk_audio = profile_audio_path(entry.get("uk_audio_path"))
    us_audio = profile_audio_path(entry.get("us_audio_path"))
    acceptation = format_acceptation(senses)
    return WordRow(
        word_name=word_name,
        display_name=text_value(entry.get("word")) or word_name,
        phonetics=strip_phonetic(us or uk),
        phonetics_uk=uk,
        phonetics_us=us,
        ph_mp3=us_audio or uk_audio,
        ph_mp3_uk=uk_audio,
        ph_mp3_us=us_audio,
        acceptation=acceptation,
        # 简明释义仅作为导入默认值，取完整释义中的第一个解释。
        exchange=acceptation.split("|", 1)[0] if acceptation else None,
        parts=unique_parts(senses),
        word_forms=text_value(entry.get("word_forms")),
        usage_labels=text_value(entry.get("usage_labels")),
        variants=text_value(entry.get("variants")),
        grammar=text_value(entry.get("grammar")),
        dictionary_detail=json.dumps(senses, ensure_ascii=False, separators=(",", ":")),
    )


def extract_sentences(entry: dict[str, object]) -> Iterator[SentenceRow]:
    word_name = normalize_key(str(entry.get("lookup_key") or ""))
    order = 0
    for sense in entry.get("senses") or []:
        sense_id = text_value(sense.get("sense_id"))
        for example in sense.get("examples") or []:
            orig = text_value(example.get("text"))
            if not orig:
                continue
            order += 1
            yield SentenceRow(
                word_name=word_name,
                sense_id=sense_id,
                sort_order=order,
                orig=orig,
                trans=text_value(example.get("translation")),
                audio_path=profile_audio_path(example.get("audio_path")),
            )


def build_alias_rows(
    entries: Sequence[dict[str, object]], raw_aliases: Sequence[dict[str, object]]
) -> tuple[tuple[AliasRow, ...], int]:
    """解析多级、多目标别名，并按规范化检索键去重。"""
    word_keys = {normalize_key(str(entry.get("lookup_key") or "")) for entry in entries}
    targets_by_alias: dict[str, list[str]] = defaultdict(list)
    raw_target_by_pair: dict[tuple[str, str], str] = {}
    raw_alias_by_key: dict[str, str] = {}
    for relation in raw_aliases:
        raw_alias = str(relation.get("alias_key") or "")
        raw_target = str(relation.get("target_key") or "")
        alias_key = normalize_key(raw_alias)
        target_key = normalize_key(raw_target)
        if not alias_key or not target_key:
            continue
        if target_key not in targets_by_alias[alias_key]:
            targets_by_alias[alias_key].append(target_key)
        raw_target_by_pair.setdefault((alias_key, target_key), raw_target)
        raw_alias_by_key.setdefault(alias_key, raw_alias)

    cache: dict[str, tuple[str, ...]] = {}

    def resolve(key: str, trail: tuple[str, ...] = ()) -> tuple[str, ...]:
        if key in word_keys:
            return (key,)
        if key in cache:
            return cache[key]
        if key in trail:
            return ()
        resolved: list[str] = []
        for target in targets_by_alias.get(key, []):
            for final_key in resolve(target, trail + (key,)):
                if final_key not in resolved:
                    resolved.append(final_key)
        cache[key] = tuple(resolved)
        return cache[key]

    # 词条内 aliases 是导出器已经解析过的完整关系，用它记录源关系总量。
    source_relations: list[tuple[str, str, str, str]] = []
    for entry in entries:
        for relation in entry.get("aliases") or []:
            raw_alias = str(relation.get("alias_key") or "")
            raw_target = str(relation.get("target_key") or "")
            resolved = normalize_key(str(relation.get("resolved_key") or entry.get("lookup_key") or ""))
            if raw_alias and raw_target and resolved:
                source_relations.append((raw_alias, raw_target, normalize_key(raw_alias), resolved))
    source_count = len({(raw_alias, resolved) for raw_alias, _, _, resolved in source_relations})

    # 同时解析原始别名文件，补上不在词条 aliases 中但可定位到正式词条的关系。
    candidates = list(source_relations)
    for alias_key, targets in targets_by_alias.items():
        for final_key in resolve(alias_key):
            raw_target = raw_target_by_pair.get((alias_key, targets[0]), targets[0])
            candidates.append((raw_alias_by_key[alias_key], raw_target, alias_key, final_key))

    rows_by_pair: dict[tuple[str, str], AliasRow] = {}
    next_order: dict[str, int] = defaultdict(int)
    for raw_alias, raw_target, alias_key, target_key in candidates:
        pair = (alias_key, target_key)
        if not alias_key or target_key not in word_keys or pair in rows_by_pair:
            continue
        next_order[alias_key] += 1
        rows_by_pair[pair] = AliasRow(
            alias_key=alias_key,
            raw_alias_key=raw_alias,
            raw_target_key=raw_target,
            target_key=target_key,
            sort_order=next_order[alias_key],
        )
    return tuple(rows_by_pair.values()), source_count


def collect_audio_paths(entries: Sequence[dict[str, object]], source_dir: Path) -> tuple[tuple[Path, ...], tuple[Path, ...]]:
    relative_paths: set[Path] = set()
    for entry in entries:
        for field in ("uk_audio_path", "us_audio_path"):
            value = text_value(entry.get(field))
            if value:
                relative_paths.add(Path(value))
        for sense in entry.get("senses") or []:
            for example in sense.get("examples") or []:
                value = text_value(example.get("audio_path"))
                if value:
                    relative_paths.add(Path(value))
    missing = tuple(sorted(path for path in relative_paths if not (source_dir / path).is_file()))
    return tuple(sorted(relative_paths)), missing


def load_import_data(source_dir: Path) -> ImportData:
    with (source_dir / "词条.json").open(encoding="utf-8") as handle:
        entries = json.load(handle)
    with (source_dir / "别名.json").open(encoding="utf-8") as handle:
        raw_aliases = json.load(handle)
    if not isinstance(entries, list) or not isinstance(raw_aliases, list):
        raise ValueError("词条.json 和 别名.json 顶层必须是数组")

    words = tuple(to_word_row(entry) for entry in entries)
    if len({word.word_name for word in words}) != len(words):
        raise ValueError("规范化后的 lookup_key 存在重复")
    sentences = tuple(sentence for entry in entries for sentence in extract_sentences(entry))
    aliases, source_alias_count = build_alias_rows(entries, raw_aliases)
    referenced_audio, missing_audio = collect_audio_paths(entries, source_dir)
    return ImportData(
        words=words,
        sentences=sentences,
        aliases=aliases,
        sense_count=sum(len(entry.get("senses") or []) for entry in entries),
        source_alias_relation_count=source_alias_count,
        referenced_audio=referenced_audio,
        missing_audio=missing_audio,
    )


def sql_literal(value: object) -> str:
    """生成 mysql 客户端可安全解析的 UTF-8 SQL 字面量。"""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, int):
        return str(value)
    text = str(value)
    escaped = (text.replace("\\", "\\\\").replace("'", "\\'")
               .replace("\0", "\\0").replace("\n", "\\n").replace("\r", "\\r")
               .replace("\x1a", "\\Z"))
    return f"'{escaped}'"


def chunks(values: Sequence[object], size: int) -> Iterator[Sequence[object]]:
    for offset in range(0, len(values), size):
        yield values[offset:offset + size]


def mysql_command(args: argparse.Namespace) -> list[str]:
    return [
        args.mysql_binary, "--protocol=TCP", "--batch", "--raw", "--skip-column-names",
        "--default-character-set=utf8mb4", f"--host={args.host}", f"--port={args.port}",
        f"--user={args.user}", args.database,
    ]


def mysql_environment() -> dict[str, str]:
    password = os.environ.get("BETTA_DB_PASSWORD")
    if password is None:
        raise RuntimeError("正式导入必须通过 BETTA_DB_PASSWORD 环境变量提供数据库密码")
    environment = os.environ.copy()
    environment["MYSQL_PWD"] = password
    return environment


def execute_sql(args: argparse.Namespace, sql: str, capture: bool = False) -> str:
    result = subprocess.run(
        mysql_command(args), input=sql, text=True, encoding="utf-8",
        stdout=subprocess.PIPE if capture else subprocess.DEVNULL,
        stderr=subprocess.PIPE, env=mysql_environment(), check=False,
    )
    if result.returncode:
        error = (result.stderr or "mysql 执行失败").strip()
        raise RuntimeError(error)
    return result.stdout if capture else ""


def word_batch_sql(rows: Sequence[WordRow]) -> str:
    columns = (
        "word_name,display_name,phonetics,phonetics_uk,phonetics_us,ph_mp3,ph_mp3_uk,ph_mp3_us,"
        "acceptation,exchange,parts,word_forms,usage_labels,variants,grammar,dictionary_source,dictionary_detail,"
        "status,create_by,create_time"
    )
    values = []
    for row in rows:
        values.append("(" + ",".join(sql_literal(value) for value in (
            row.word_name, row.display_name, row.phonetics, row.phonetics_uk, row.phonetics_us,
            row.ph_mp3, row.ph_mp3_uk, row.ph_mp3_us, row.acceptation, row.exchange, row.parts,
            row.word_forms, row.usage_labels, row.variants, row.grammar, SOURCE, row.dictionary_detail, "0",
            "oald10-import",)) + ",NOW())")
    preserve_if_empty = (
        "display_name", "phonetics", "phonetics_uk", "phonetics_us", "ph_mp3", "ph_mp3_uk",
        "ph_mp3_us", "acceptation", "parts", "word_forms", "usage_labels", "variants", "grammar",
    )
    assignments = [f"{column}=COALESCE(NULLIF(VALUES({column}),''),{column})" for column in preserve_if_empty]
    assignments.extend((
        "exchange=COALESCE(NULLIF(exchange,''),VALUES(exchange))",
        "dictionary_source=VALUES(dictionary_source)",
        "dictionary_detail=VALUES(dictionary_detail)",
        "update_by='oald10-import'", "update_time=NOW()",
    ))
    return ("START TRANSACTION;\nINSERT INTO eng_word(" + columns + ") VALUES\n" +
            ",\n".join(values) + "\nON DUPLICATE KEY UPDATE " + ",".join(assignments) +
            ";\nCOMMIT;\n")


def fetch_word_ids(args: argparse.Namespace) -> dict[str, int]:
    output = execute_sql(args, "SELECT id,HEX(word_name) FROM eng_word;\n", capture=True)
    result: dict[str, int] = {}
    for line in output.splitlines():
        identifier, hex_name = line.split("\t", 1)
        result[bytes.fromhex(hex_name).decode("utf-8")] = int(identifier)
    return result


def sentence_batch_sql(
    rows: Sequence[SentenceRow], word_ids: dict[str, int], word_names: Sequence[str] | None = None
) -> str:
    affected_names = word_names if word_names is not None else list(dict.fromkeys(row.word_name for row in rows))
    grouped_ids = sorted({word_ids[word_name] for word_name in affected_names})
    statements = ["START TRANSACTION;"]
    statements.append(
        "DELETE FROM eng_iciba_sentence WHERE source='oald10' AND word_id IN (" +
        ",".join(map(str, grouped_ids)) + ");"
    )
    values = []
    for row in rows:
        values.append("(" + ",".join(sql_literal(value) for value in (
            word_ids[row.word_name], row.orig, row.trans, SOURCE, row.sense_id, row.sort_order,
            row.audio_path, "0", "oald10-import",)) + ",NOW())")
    if values:
        statements.append(
            "INSERT INTO eng_iciba_sentence(word_id,orig,trans,source,sense_id,sort_order,audio_path,"
            "status,create_by,create_time) VALUES\n" + ",\n".join(values) + ";"
        )
    statements.append("COMMIT;")
    return "\n".join(statements) + "\n"


def alias_batch_sql(rows: Sequence[AliasRow], word_ids: dict[str, int]) -> str:
    alias_keys = list(dict.fromkeys(row.alias_key for row in rows))
    values = []
    for row in rows:
        values.append("(" + ",".join(sql_literal(value) for value in (
            row.alias_key, row.raw_alias_key, row.raw_target_key, word_ids[row.target_key], SOURCE,
            row.sort_order,)) + ",NOW())")
    return (
        "START TRANSACTION;\nDELETE FROM eng_word_alias WHERE source='oald10' AND alias_key IN (" +
        ",".join(sql_literal(key) for key in alias_keys) + ");\n" +
        "INSERT INTO eng_word_alias(alias_key,raw_alias_key,raw_target_key,word_id,source,sort_order,"
        "create_time) VALUES\n" + ",\n".join(values) + ";\nCOMMIT;\n"
    )


def copy_audio(source_dir: Path, destination: Path, paths: Iterable[Path]) -> tuple[int, int, int]:
    copied = skipped = replaced = 0
    for relative in paths:
        source = source_dir / relative
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            if source.stat().st_size == target.stat().st_size:
                skipped += 1
                continue
            # 大小冲突时再比对摘要，摘要不同才覆盖。
            source_hash = hashlib.sha256(source.read_bytes()).digest()
            target_hash = hashlib.sha256(target.read_bytes()).digest()
            if source_hash == target_hash:
                skipped += 1
                continue
            replaced += 1
        else:
            copied += 1
        shutil.copy2(source, target)
    return copied, skipped, replaced


def import_database(args: argparse.Namespace, data: ImportData) -> None:
    for batch in chunks(data.words, args.batch_size):
        execute_sql(args, word_batch_sql(batch))
    word_ids = fetch_word_ids(args)
    missing_words = [word.word_name for word in data.words if word.word_name not in word_ids]
    if missing_words:
        raise RuntimeError(f"词条写入后缺少 {len(missing_words)} 个主键，首个为 {missing_words[0]!r}")

    sentences_by_word: dict[str, list[SentenceRow]] = defaultdict(list)
    for sentence in data.sentences:
        sentences_by_word[sentence.word_name].append(sentence)
    for word_batch in chunks(data.words, args.batch_size):
        word_names = [word.word_name for word in word_batch]
        rows = [row for word_name in word_names for row in sentences_by_word[word_name]]
        execute_sql(args, sentence_batch_sql(rows, word_ids, word_names))

    aliases_by_key: dict[str, list[AliasRow]] = defaultdict(list)
    for alias in data.aliases:
        aliases_by_key[alias.alias_key].append(alias)
    execute_sql(args, "START TRANSACTION; DELETE FROM eng_word_alias WHERE source='oald10'; COMMIT;\n")
    alias_groups = list(aliases_by_key.values())
    for batch in chunks(alias_groups, args.batch_size):
        rows = [row for group in batch for row in group]
        execute_sql(args, alias_batch_sql(rows, word_ids))


def print_report(data: ImportData) -> None:
    print(f"词条: {len(data.words)}")
    print(f"义项: {data.sense_count}")
    print(f"例句: {len(data.sentences)}")
    print(f"源别名关系: {data.source_alias_relation_count}")
    print(f"规范化后唯一别名关系: {len(data.aliases)}")
    print(f"音频引用: {len(data.referenced_audio)}")
    print(f"缺失音频: {len(data.missing_audio)}")
    if data.missing_audio:
        print("首个缺失音频:", data.missing_audio[0])


def parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE_DIR)
    parser.add_argument("--audio-destination", type=Path, default=DEFAULT_AUDIO_DESTINATION)
    parser.add_argument("--dry-run", action="store_true", help="只校验数据和音频，不写数据库或复制文件")
    parser.add_argument("--skip-audio", action="store_true", help="正式导入时跳过音频复制")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--database", default="betta1.0")
    parser.add_argument("--user", default="root")
    parser.add_argument("--mysql-binary", default="mysql")
    parser.add_argument("--batch-size", type=int, default=200)
    args = parser.parse_args(argv)
    if args.batch_size < 1:
        parser.error("--batch-size 必须大于 0")
    return args


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv)
    data = load_import_data(args.source_dir)
    print_report(data)
    if data.missing_audio:
        return 2
    if args.dry_run:
        print("dry-run 校验通过，未写数据库或复制音频。")
        return 0
    if not args.skip_audio:
        copied, skipped, replaced = copy_audio(args.source_dir, args.audio_destination, data.referenced_audio)
        print(f"音频复制完成: 新增 {copied}，跳过 {skipped}，替换 {replaced}")
    import_database(args, data)
    print("数据库导入完成。")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, RuntimeError, json.JSONDecodeError) as error:
        print(f"导入失败: {error}", file=sys.stderr)
        sys.exit(1)
