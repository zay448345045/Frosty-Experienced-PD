'''
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
'''
import os
import sys
import time
import shutil
import subprocess
import threading
import argparse
from pathlib import Path
from queue import Queue, Empty
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from lxml import etree

class XMLBatchDownloader:
    def __init__(self, base_url="https://wazone-file.wahlap.net/", max_workers=10, output_dir="harvest_zone"):
        self.base_url = base_url.rstrip('/')
        self.max_workers = max_workers
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 统计信息
        self.stats = {'downloaded': 0, 'failed': 0, 'skipped': 0}
        self.stop_signal = False
        self.lock = threading.Lock()
        
        # 模拟 Unity 客户端请求，防止 403
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'UnityPlayer/2021.3.11f1c1 (UnityWebRequest/1.0)',
            'Referer': 'https://wazone.wahlap.net/'
        })

    def check_disk_space(self):
        """
        磁盘安全策略：
        因为 GitHub Action 只有约 30GB 空间。
        在打包（mx0模式）时，磁盘占用会翻倍。
        因此原始文件下到 45% (约 13-14GB) 必须强制停止。
        """
        total, used, free = shutil.disk_usage("/")
        usage_percent = (used / total) * 100
        if usage_percent > 45:
            print(f"\n[⚠️ 熔断] 磁盘占用已达 {usage_percent:.1f}%。为确保打包安全，本轮收割到此结束。")
            self.stop_signal = True
            return True
        return False

    def parse_xml_file(self, xml_path: Path):
        """解析单个华为云/OBS格式的 XML 清单"""
        try:
            with open(xml_path, 'r', encoding='utf-8') as f:
                xml_content = f.read()
            root = etree.fromstring(xml_content.encode('utf-8'))
            namespaces = {'ns': 'http://obs.myhwclouds.com/doc/2015-06-30/'}
            
            contents = root.xpath('.//ns:Contents', namespaces=namespaces) or root.xpath('.//Contents')
            
            file_list = []
            for item in contents:
                key_elem = item.find('Key') if item.find('Key') is not None else item.find('ns:Key', namespaces=namespaces)
                size_elem = item.find('Size') if item.find('Size') is not None else item.find('ns:Size', namespaces=namespaces)
                
                if key_elem is not None and size_elem is not None:
                    key = key_elem.text.strip()
                    size = int(size_elem.text)
                    if size > 0 and not key.endswith('/'):
                        file_list.append((key, size))
            return file_list
        except Exception as e:
            print(f"  ❌ 解析 {xml_path.name} 失败: {e}")
            return []

    def download_file(self, key, size):
        if self.stop_signal:
            return False
            
        file_url = f"{self.base_url}/{key}"
        local_path = self.output_dir / key
        
        # 即使虚拟机是空的，这里也保留逻辑以防万一
        if local_path.exists() and local_path.stat().st_size == size:
            with self.lock: self.stats['skipped'] += 1
            return True

        local_path.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            # 使用流式下载节省内存
            with self.session.get(file_url, stream=True, timeout=(10, 30)) as r:
                r.raise_for_status()
                with open(local_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=1024*64):
                        if self.stop_signal: return False
                        if chunk: f.write(chunk)
            with self.lock: self.stats['downloaded'] += 1
            return True
        except Exception as e:
            # 只有在非熔断情况下才打印错误
            if not self.stop_signal:
                print(f"  [-] 下载失败 {key}: {e}")
                with self.lock: self.stats['failed'] += 1
            return False

    def run(self, xml_dir, skip_count):
        # 1. 扫描并解析所有 XML
        xml_files = sorted(Path(xml_dir).glob("*.xml"))
        if not xml_files:
            print(f"❌ 在 {xml_dir} 目录下没找到 XML 文件！")
            return

        all_assets = []
        for x in xml_files:
            all_assets.extend(self.parse_xml_file(x))
        
        total_items = len(all_assets)
        # 2. 应用跳过逻辑
        task_assets = all_assets[skip_count:]
        
        print(f"\n{'='*50}")
        print(f"🚀 任务启动 | 总清单项: {total_items}")
        print(f"📍 本次起点: 第 {skip_count} 项 | 待处理: {len(task_assets)}")
        print(f"{'='*50}\n")

        # 3. 线程池收割
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_key = {executor.submit(self.download_file, k, s): k for k, s in task_assets}
            
            processed_count = 0
            for future in as_completed(future_to_key):
                processed_count += 1
                
                # 每 50 个文件检查一次磁盘
                if processed_count % 50 == 0:
                    if self.check_disk_space():
                        # 触发停止信号后，不再提交新任务，等待现有任务结束
                        break
            
            # 统计最终进度
            final_index = skip_count + processed_count
            print(f"\n{'='*50}")
            print(f"🏁 本轮收割尝试完毕！")
            print(f"✅ 成功: {self.stats['downloaded']} | ⏭️ 跳过: {self.stats['skipped']} | ❌ 失败: {self.stats['failed']}")
            print(f"📢 下一轮运行，请在 skip_to 框内填入数字: {final_index}")
            print(f"{'='*50}\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Wazone XML Downloader")
    parser.add_argument('--skip', type=int, default=0, help='要跳过的文件数量')
    parser.add_argument('--xml_dir', type=str, default='./lists', help='XML清单所在目录')
    args = parser.parse_args()

    downloader = XMLBatchDownloader(output_dir="harvest_zone")
    downloader.run(xml_dir=args.xml_dir, skip_count=args.skip)
