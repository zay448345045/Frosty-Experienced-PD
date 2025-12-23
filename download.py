import os
import sys
import time
import shutil
import argparse
import threading
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from lxml import etree

class XMLBatchDownloader:
    def __init__(self, base_url="https://wazone-file.wahlap.net/", max_workers=15, output_dir="harvest_zone"):
        self.base_url = base_url.rstrip('/')
        self.max_workers = max_workers
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

        self.stats = {'downloaded': 0, 'failed': 0, 'skipped': 0, 'retried': 0}
        self.failed_keys = []  # 记录具体的失败名单
        self.stop_signal = False
        self.lock = threading.Lock()

        # 伪装 Unity 客户端请求
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'UnityPlayer/2021.3.11f1c1 (UnityWebRequest/1.0)',
            'Referer': 'https://wazone.wahlap.net/'
        })

    def check_disk_space(self):
        """核心监控：剩余空间低于 45GB 则强制收尾（确保 7z 有足够镜像空间）"""
        total, used, free = shutil.disk_usage("/")
        free_gb = free / (1024**3)

        if self.stats['downloaded'] % 50 == 0:
            print(f"📊 空间监测：剩余 {free_gb:.2f} GB 可用 | 已下: {self.stats['downloaded']}")

        if free_gb < 30:
            print(f"\n[⚠️ 空间熔断] 磁盘仅剩 {free_gb:.2f} GB（触碰 45GB 安全线），强制停止并准备打包。")
            self.stop_signal = True
            return True
        return False

    def parse_xml_file(self, xml_path: Path):
        """解析 XML 资源清单"""
        try:
            with open(xml_path, 'r', encoding='utf-8') as f:
                root = etree.fromstring(f.read().encode('utf-8'))
            namespaces = {'ns': 'http://obs.myhwclouds.com/doc/2015-06-30/'}
            contents = root.xpath('.//ns:Contents', namespaces=namespaces) or root.xpath('.//Contents')

            file_list = []
            for item in contents:
                k = item.find('Key') if item.find('Key') is not None else item.find('ns:Key', namespaces=namespaces)
                s = item.find('Size') if item.find('Size') is not None else item.find('ns:Size', namespaces=namespaces)
                if k is not None and s is not None:
                    key, size = k.text.strip(), int(s.text)
                    if size > 0 and not key.endswith('/'):
                        file_list.append((key, size))
            return file_list
        except Exception as e:
            print(f"  ❌ 解析 {xml_path.name} 失败: {e}")
            return []

    def download_file(self, key, size, max_retries=3):
        """执行下载：具备断点跳过与完整性校验"""
        if self.stop_signal: return False

        file_url = f"{self.base_url}/{key}"
        local_path = self.output_dir / key

        # 1. 存在性校验
        if local_path.exists():
            if local_path.stat().st_size == size:
                with self.lock: self.stats['skipped'] += 1
                return True
            else:
                local_path.unlink()

        local_path.parent.mkdir(parents=True, exist_ok=True)

        # 2. 重试下载逻辑
        for attempt in range(max_retries):
            if self.stop_signal: return False
            try:
                with self.session.get(file_url, stream=True, timeout=(10, 30)) as r:
                    r.raise_for_status()
                    with open(local_path, 'wb') as f:
                        for chunk in r.iter_content(chunk_size=1024*128):
                            if self.stop_signal: 
                                f.close()
                                if local_path.exists(): local_path.unlink()
                                return False
                            if chunk: f.write(chunk)
                    
                    if local_path.stat().st_size >= size:
                        with self.lock: self.stats['downloaded'] += 1
                        return True
            except Exception:
                if local_path.exists(): local_path.unlink()
                if attempt < max_retries - 1:
                    time.sleep(2)
                    continue
        
        # 彻底失败后记录
        with self.lock:
            self.stats['failed'] += 1
            self.failed_keys.append(key)
        return False

    def run(self, xml_dir, skip_count):
        xml_files = sorted(Path(xml_dir).glob("*.xml"))
        all_assets = []
        for x in xml_files:
            all_assets.extend(self.parse_xml_file(x))

        task_assets = all_assets[skip_count:]
        print(f"\n🚀 任务启动 | 总量: {len(all_assets)} | 起始索引: {skip_count}\n")

        processed_this_round = 0
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_key = {executor.submit(self.download_file, k, s): k for k, s in task_assets}

            for future in as_completed(future_to_key):
                processed_this_round += 1
                if processed_this_round % 15 == 0:
                    if self.check_disk_space():
                        break

        # --- 核心：保存情报与进度 ---
        final_index = skip_count + processed_this_round
        
        # 1. 保存下一轮起始索引
        with open("next_skip.txt", "w") as f:
            f.write(str(final_index))
        
        # 2. 保存失败报告
        if self.failed_keys:
            with open("failed_report.txt", "w") as f:
                f.write("\n".join(self.failed_keys))

        print(f"\n" + "="*60)
        print(f"🏁 阶段性总结")
        print(f"✅ 成功: {self.stats['downloaded']} | ⏭️ 跳过: {self.stats['skipped']} | ❌ 失败: {self.stats['failed']}")
        print(f"📢 自动接力点（下次 --skip 参数）: {final_index}")
        print("="*60 + "\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--skip', type=int, default=0)
    parser.add_argument('--xml_dir', type=str, default='./lists')
    parser.add_argument('--workers', type=int, default=15)
    args = parser.parse_args()

    downloader = XMLBatchDownloader(output_dir="harvest_zone", max_workers=args.workers)
    downloader.run(xml_dir=args.xml_dir, skip_count=args.skip)
