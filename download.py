import os, sys, time, shutil, subprocess, threading
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from lxml import etree

class XMLBatchDownloader:
    def __init__(self, base_url="https://wazone-file.wahlap.net/", max_workers=10, output_dir="harvest_zone"):
        self.base_url = base_url.rstrip('/')
        self.max_workers = max_workers
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.session = requests.Session()
        self.session.headers.update({'User-Agent': 'UnityPlayer/2021.3.11f1c1 (UnityWebRequest/1.0)'})
        self.stop_signal = False

    def check_disk_space(self):
        """核心：确保原始文件 + 7z包 <= 30GB"""
        total, used, free = shutil.disk_usage("/")
        usage = (used / total) * 100
        if usage > 45: # 安全熔断水位线
            print(f"\n[!] 磁盘占用已达 {usage:.1f}%，为确保打包安全，停止下载。")
            self.stop_signal = True
            return True
        return False

    def parse_xml_file(self, xml_path):
        try:
            tree = etree.parse(str(xml_path))
            root = tree.getroot()
            ns = {'ns': 'http://obs.myhwclouds.com/doc/2015-06-30/'}
            contents = root.xpath('.//ns:Contents', namespaces=ns) or root.xpath('.//Contents')
            files = []
            for item in contents:
                key = (item.find('ns:Key', namespaces=ns) if item.find('ns:Key', namespaces=ns) is not None else item.find('Key')).text
                size = int((item.find('ns:Size', namespaces=ns) if item.find('ns:Size', namespaces=ns) is not None else item.find('Size')).text)
                if size > 0 and not key.endswith('/'):
                    files.append((key, size))
            return files
        except Exception as e:
            print(f"  ❌ 解析 {xml_path.name} 失败: {e}")
            return []

    def download_file(self, key, size):
        if self.stop_signal: return False
        file_url = f"{self.base_url}/{key}"
        local_path = self.output_dir / key
        
        if local_path.exists() and local_path.stat().st_size == size:
            return True # 跳过已下载

        local_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            with self.session.get(file_url, stream=True, timeout=30) as r:
                r.raise_for_status()
                with open(local_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        if self.stop_signal: return False
                        f.write(chunk)
            return True
        except Exception as e:
            print(f"  [-] 失败 {key}: {e}")
            return False

    def start(self, xml_dir,skip_offset=0):
        xml_files = sorted(Path(xml_dir).glob("*.xml"))
        all_assets = []
        for x in xml_files:
            all_assets.extend(self.parse_xml_file(x))
        task_assets = all_assets[skip_offset:]
        print(f"🚀 总计清单项: {len(all_assets)}。已跳过 {skip_offset}，本次待处理 {len(task_assets)} 开始并发同步...")
        
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_key = {executor.submit(self.download_file, k, s): k for k, s in all_assets}
            count = 0
            for future in as_completed(future_to_key):
                count += 1
                if count % 50 == 0 and self.check_disk_space():
                    break
        print("--- 本轮下载线程池已关闭 ---")

if __name__ == "__main__":
    downloader = XMLBatchDownloader(output_dir="harvest_zone")
    downloader.start(xml_dir="./lists")
