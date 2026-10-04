#!/usr/bin/env python3
"""牛津词典导入器的标准库回归测试。"""

from __future__ import annotations

import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("import_oald10_dictionary.py")
SPEC = importlib.util.spec_from_file_location("import_oald10_dictionary", MODULE_PATH)
assert SPEC and SPEC.loader
IMPORTER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = IMPORTER
SPEC.loader.exec_module(IMPORTER)


class ImportOald10DictionaryTest(unittest.TestCase):
    def test_alias_key_migration_uses_binary_collation(self) -> None:
        migration = (Path(__file__).parents[1] / "sql" / "1.0.15_oald10_dictionary.sql").read_text(
            encoding="utf-8"
        )
        # 应用已先做 NFKC 和小写转换，数据库必须继续区分 σ/ς 等不同规范化字符。
        self.assertIn(
            "`alias_key` VARCHAR(200) COLLATE utf8mb4_bin NOT NULL",
            migration,
        )
        self.assertNotEqual(IMPORTER.normalize_key("σ"), IMPORTER.normalize_key("ς"))

    def test_normalize_key_uses_nfkc_trim_and_lowercase(self) -> None:
        self.assertEqual("abc 1", IMPORTER.normalize_key("  ＡＢＣ １  "))

    def test_format_acceptation_keeps_pos_and_chinese_definition_order(self) -> None:
        senses = [
            {"pos": "noun", "definition": {"zh": "水果", "en": "a fruit"}},
            {"pos": "", "definition": {"zh": "甜味", "en": "sweet taste"}},
            {"pos": "verb", "definition": {"en": "to fruit"}},
        ]
        self.assertEqual("noun 水果|甜味", IMPORTER.format_acceptation(senses))

    def test_word_row_uses_first_chinese_definition_as_default_exchange(self) -> None:
        row = IMPORTER.to_word_row({
            "lookup_key": "Apple",
            "senses": [
                {"pos": "noun", "definition": {"zh": "水果"}},
                {"definition": {"zh": "苹果公司相关内容"}},
            ],
        })
        self.assertEqual("noun 水果|苹果公司相关内容", row.acceptation)
        self.assertEqual("noun 水果", row.exchange)

        row_without_chinese_definition = IMPORTER.to_word_row({
            "lookup_key": "fruit",
            "senses": [{"pos": "verb", "definition": {"en": "to produce fruit"}}],
        })
        self.assertIsNone(row_without_chinese_definition.exchange)

    def test_alias_resolution_supports_chains_and_multiple_targets(self) -> None:
        entries = [
            {"lookup_key": "target-a", "aliases": []},
            {"lookup_key": "target-b", "aliases": []},
        ]
        raw_aliases = [
            {"alias_key": "Root", "target_key": "middle"},
            {"alias_key": "middle", "target_key": "target-a"},
            {"alias_key": "middle", "target_key": "target-b"},
        ]
        rows, source_count = IMPORTER.build_alias_rows(entries, raw_aliases)
        self.assertEqual(0, source_count)
        self.assertEqual(
            [("root", "target-a", 1), ("root", "target-b", 2)],
            [(row.alias_key, row.target_key, row.sort_order) for row in rows if row.alias_key == "root"],
        )

    def test_sql_literal_escapes_quotes_backslashes_and_line_breaks(self) -> None:
        self.assertEqual("'a\\'b\\\\c\\nd'", IMPORTER.sql_literal("a'b\\c\nd"))

    def test_word_sql_is_idempotent_and_preserves_empty_source_fields(self) -> None:
        row = IMPORTER.WordRow(
            word_name="word", display_name="Word", phonetics=None, phonetics_uk=None,
            phonetics_us=None, ph_mp3=None, ph_mp3_uk=None, ph_mp3_us=None,
            acceptation="释义｜definition", exchange="释义", parts="noun", word_forms=None,
            usage_labels=None, variants=None, grammar=None, dictionary_detail="[]",
        )
        sql = IMPORTER.word_batch_sql([row])
        self.assertIn("ON DUPLICATE KEY UPDATE", sql)
        self.assertIn("acceptation,exchange,parts", sql)
        self.assertIn("acceptation=COALESCE(NULLIF(VALUES(acceptation),''),acceptation)", sql)
        self.assertIn("exchange=COALESCE(NULLIF(exchange,''),VALUES(exchange))", sql)
        self.assertIn("START TRANSACTION", sql)
        self.assertIn("COMMIT", sql)

    def test_missing_audio_is_reported_without_requiring_optional_audio(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            existing = source / "pron_us_mp3" / "word.mp3"
            existing.parent.mkdir()
            existing.write_bytes(b"audio")
            entries = [{
                "uk_audio_path": "pron_uk_mp3/missing.mp3",
                "us_audio_path": "pron_us_mp3/word.mp3",
                "senses": [{"examples": [{"audio_path": None}]}],
            }]
            referenced, missing = IMPORTER.collect_audio_paths(entries, source)
        self.assertEqual(2, len(referenced))
        self.assertEqual((Path("pron_uk_mp3/missing.mp3"),), missing)

    def test_sentence_batch_deletes_previous_source_rows_before_insert(self) -> None:
        row = IMPORTER.SentenceRow("word", "sense-1", 1, "Example.", "例句。", None)
        sql = IMPORTER.sentence_batch_sql([row], {"word": 7}, ["word"])
        self.assertLess(sql.index("DELETE FROM eng_iciba_sentence"), sql.index("INSERT INTO eng_iciba_sentence"))
        self.assertIn("source='oald10' AND word_id IN (7)", sql)


if __name__ == "__main__":
    unittest.main()
