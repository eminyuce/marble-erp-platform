/**
 * Özerler Mermer ERP - 10 Adımda Öğrenme Merkezi (Alpine.js Component)
 */
function createLearningCenter() {
    return {
        activeCategory: 'Tümü',
        selectedRole: 'all', // 'all', 'operator', 'manager'
        demoMode: false,
        expandedCard: 'W1', // open first workflow card by default
        doneWorkflows: {},
        votes: {},

        // 5 Outcome-based checklist items mapped to workflows
        checklist: [
            {
                id: 'c1',
                title: 'İlk bloğu üret',
                mappedWorkflow: 'W1 (Ocak Blok Üretim)',
                targetWfId: 'W1',
                hint: 'Ocak üretim formunda en, boy, yükseklik girip bloğu Üretim Sahası\'na kaydedin.',
                completed: false
            },
            {
                id: 'c2',
                title: 'Bir bloğu fabrikaya sevk et',
                mappedWorkflow: 'W2 (Stok & Fabrikaya Sevk)',
                targetWfId: 'W2',
                hint: 'Gerçek tartım kilosu ve nakliye ücreti ile bloğu Fabrika Blok Stok Sahası\'na aktarın.',
                completed: false
            },
            {
                id: 'c3',
                title: 'Bir kesim + cila operasyonu yap',
                mappedWorkflow: 'W4 + W5 (Kesim & Cila)',
                targetWfId: 'W4',
                hint: 'Katrak veya ST ile kesim emri açıp ardından Plaka Cila çıktısını onaylayın.',
                completed: false
            },
            {
                id: 'c4',
                title: 'Bir paleti atölyede ebatlayıp sat',
                mappedWorkflow: 'W7 (Atölye Ebatlama & Satış)',
                targetWfId: 'W7',
                hint: 'Atölye iş emri oluşturup müşteriye satış veya şantiyeye sevk olarak tamamlayın.',
                completed: false
            },
            {
                id: 'c5',
                title: 'İlk aylık maliyet raporunu gör',
                mappedWorkflow: 'W10 (Aylık Gerçek Maliyet)',
                targetWfId: 'W10',
                hint: 'Elektrik faturası ve giderleri ait olduğu aya girip m² ve ton başına net maliyeti hesaplayın.',
                completed: false
            }
        ],

        // 10 Workflows dataset strictly matching the requirements
        workflows: [
            {
                id: "W1",
                kategori: "Ocak",
                baslik: "Blok Üretim ve Numaralandırma",
                amac: "Ocakta çıkan bloğu doğru tonajla kaydetmek ve izlenebilir kılmak",
                on_kosul: "Ocak bölgeleri ve özgül ağırlık tablosu tanımlı olmalı",
                adimlar: [
                    "Ocak > Yeni Blok Üret > Bölge seç (örn: Ocak-A Blok)",
                    "Sistem numara verir: Bölge-Kodu + Tarih + Sıra (A-240521-001)",
                    "En, Boy, Yükseklik gir > Sistem Tahmini Tonaj = En x Boy x Yük x Özgül Ağırlık",
                    "Stok yeri seç: Üretim Sahası",
                    "Kaydet"
                ],
                sonuc: "Blok Üretim Sahası'nda tahmini tonajla listelenir",
                sik_hata: "En/Boy/Yükseklik birimi karışması - hep Metre kullan",
                hedef_sayfa: "/ocak/uretim/yeni",
                sure: "2 dk",
                zorluk: "Başlangıç",
                role: "operator",
                demo_data: "Bölge: Ocak-A Blok | En: 2.80 m, Boy: 1.60 m, Yükseklik: 1.40 m | Özgül Ağırlık: 2.70 t/m³ | Hesaplanan Tonaj: 16.93 Ton | Stok Sahası: Üretim Sahası"
            },
            {
                id: "W2",
                kategori: "Ocak",
                baslik: "Stok Hareketi, Blok Satışı ve Fabrikaya Sevk",
                amac: "2 stok yerini yönetmek ve gerçek kiloyu girmek",
                on_kosul: "W1 tamamlanmış olmalı",
                adimlar: [
                    "Ocak > Stok Sahaları > Üretim Sahası'ndaki bloğu seç > İşlemler",
                    "Taşı: Hedef saha seç: Sevk Sahası",
                    "Sat: Müşteri, markalama, fiyat, saha bilgisi gir",
                    "Fabrikaya Sevk Et: Gerçek tartım kilosu ve nakliye ücreti gir > Sistem Ocak'tan düşer Fabrika Blok Stok'a ekler, nakliye fabrika maliyetine gider"
                ],
                sonuc: "Hangi bloğun hangi sahada ve kime satıldığı görünür",
                sik_hata: "Gerçek tartım girilmeden sevk edilmesi",
                hedef_sayfa: "/ocak/stok",
                sure: "3 dk",
                zorluk: "Orta",
                role: "operator",
                demo_data: "Seçilen Blok: A-240521-001 | İşlem: Fabrikaya Sevk | Gerçek Tartım: 17.200 kg | Nakliye: 4.500 TL | Hedef: Fabrika Blok Stok Sahası"
            },
            {
                id: "W3",
                kategori: "Ocak",
                baslik: "Makine Bazlı Mazot ve Günlük Gider",
                amac: "Operatörün tabletten 30 saniyede veri girmesi",
                on_kosul: "Ocak makineleri ve gider kalemleri tanımlı olmalı",
                adimlar: [
                    "Giderler ve Satın Alma > Birim: Ocak > Tarih ve Makine seç > Mazot litre gir",
                    "Diğer kalemler: Elektrik, İşçilik, Sarf, Bakım, SSK/Vergi, Diğer"
                ],
                sonuc: "Hangi makine ne kadar yaktı izlenir, W10'a akar",
                sik_hata: "Tarih seçmeden giriş yapmak",
                hedef_sayfa: "/giderler/yeni",
                sure: "1 dk",
                zorluk: "Hızlı",
                role: "operator",
                demo_data: "Birim: Ocak | Makine: CAT 336 Ekskavatör #02 | Mazot: 140 Litre | Sarf: 2 Adet Yağ Filtresi | Açıklama: A Bölgesi blok çıkarma"
            },
            {
                id: "W4",
                kategori: "Fabrika",
                baslik: "Blok Kesim - ST / Katrak",
                amac: "Giren tonaj, çıkan m2 ve fireyi yakalamak",
                on_kosul: "Fabrika stokunda kesime uygun blok ve aktif makine olmalı",
                adimlar: [
                    "Fabrika > Blok Stok > Kesime Al > Makine seç: ST veya Katrak",
                    "Giren Tonaj otomatik gelir",
                    "Çıkan m2 ve Fire gir",
                    "Operatör onayla"
                ],
                sonuc: "Verimlilik = Çıkan m2 / Giren Ton raporlanır",
                sik_hata: "ST ve Katrak makinesi karıştırılması",
                hedef_sayfa: "/fabrika/kesim",
                sure: "2 dk",
                zorluk: "Orta",
                role: "operator",
                demo_data: "Makine: Katrak-1 (80 Lama) | Blok: B-2026-0042 (24.5 Ton) | Çıkan: 198 m² Plaka | Fire: 2.1 Ton (%8.5) | Vardiya: Gündüz"
            },
            {
                id: "W5",
                kategori: "Fabrika",
                baslik: "Cila ve Fire Takibi",
                amac: "Katrak->Plaka Cila, ST->Dar Bant Cila akışını ve kaybı görmek",
                on_kosul: "W4 kesim operasyonunun tamamlanmış olması",
                adimlar: [
                    "Kesimden gelen ürünü seç > Cilaya Gönder > Makine: Plaka Cila / Dar Bant Cila",
                    "Giren m2 ve Çıkan m2 gir > Fire = Giren - Çıkan otomatik",
                    "Dar Bant'ta Pahlı/Pahsız işaretle"
                ],
                sonuc: "Makine bazlı fire oranı",
                sik_hata: "Pahlı/Pahsız işaretlenmemesi",
                hedef_sayfa: "/fabrika/cila",
                sure: "2 dk",
                zorluk: "Orta",
                role: "operator",
                demo_data: "Ürün: Katrak Plakası | Makine: 16 Kafalı Plaka Cila | Giren: 198 m² | Çıkan: 191 m² | Fire: 7 m² (%3.5) | Yüzey: Cilalı"
            },
            {
                id: "W6",
                kategori: "Fabrika",
                baslik: "Ebatlama, Pah, Paletleme ve Hazır Stok",
                amac: "Plakadan satılabilir paletli ürün yaratmak",
                on_kosul: "Cilalı plaka veya bant stoğunun hazır olması",
                adimlar: [
                    "Plaka Cila sonrası > Köprü Kesme'ye Gönder",
                    "Giren/Çıkan m2 gir",
                    "Paletle > Barkod Oluştur > Stok: Sevke Hazır Palet",
                    "Sat veya Atölyeye Sevk Et"
                ],
                sonuc: "Barkodlu, maliyeti belli stok",
                sik_hata: "Barkod oluşturmadan stoklama",
                hedef_sayfa: "/fabrika/ebatlama",
                sure: "3 dk",
                zorluk: "Orta",
                role: "operator",
                demo_data: "Ebat: 60x60x2 cm | Palet No: PLT-2026-088 | Barkod: 869001245088 | Miktar: 28.8 m² (80 adet) | Konum: Sevk Palet Sahası"
            },
            {
                id: "W7",
                kategori: "Atölye",
                baslik: "Plaka Ebatlama ve Satış / Şantiye Sevki",
                amac: "Kendi veya dış fabrikadan gelen plakayı müşteriye göre kesmek",
                on_kosul: "Atölye plaka stoğu veya dış plaka kaydı mevcut olmalı",
                adimlar: [
                    "Atölye > Yeni İş Emri > Kaynak: Kendi Fabrikam / Dış Fabrika",
                    "Gelen plakayı ve müşteri/şantiye seç",
                    "Makine: Köprü 1/2, Yan Kesme, Pah Kırma, Spirel İnce Pah",
                    "Giren/Çıkan m2 gir",
                    "Müşteriye Satıldı veya Şantiyeye Sevk Edildi olarak kapat"
                ],
                sonuc: "Atölye fire ve sevk takibi",
                sik_hata: "Yan kesme ve pah kırma karıştırılması",
                hedef_sayfa: "/atolye/is-emri",
                sure: "3 dk",
                zorluk: "Orta",
                role: "operator",
                demo_data: "Kaynak: Dış Fabrika (Marmara Beyaz) | Müşteri: Göksu Mimarlık | Makine: Köprü Kesme-2 | Giren: 5.4 m² | Çıkan: 4.8 m² | Durum: Şantiyeye Sevk Edildi"
            },
            {
                id: "W8",
                kategori: "Şantiye",
                baslik: "Proje Ön Planlama",
                amac: "Şantiyeye gitmeden ne kadar taş ve sarf gideceğini bilmek",
                on_kosul: "Müşteri sözleşmesi ve mimari mahal ölçüleri hazır olmalı",
                adimlar: [
                    "Şantiyeler > Yeni Proje > Mahalleri ekle: Lobi Zemin - Bej Traverten - 120 m2",
                    "Sistem toplam metraj çıkarır",
                    "Karar: Fabrikada Üret veya Dış Fabrikadan Sipariş",
                    "Sarf tahmini gir: Kum, Çimento, Yapıştırıcı"
                ],
                sonuc: "Sipariş listesi ve üretim planı oluşur",
                sik_hata: "Mahal bazlı metraj girmeden toplu sipariş vermek",
                hedef_sayfa: "/santiye/planlama",
                sure: "4 dk",
                zorluk: "Orta",
                role: "manager",
                demo_data: "Proje: Marina Rezidans A Blok | Mahal: Lobi Zemin (240 m² Bej Mermer) | Sarf Tahmini: 45 torba flex yapıştırıcı, 10 torba derz"
            },
            {
                id: "W9",
                kategori: "Şantiye",
                baslik: "Malzeme Geliş, Montaj ve Kar Kapatma",
                amac: "Şantiye bitince gerçek karı görmek",
                on_kosul: "W8 proje kaydı ve gelen malzeme irsaliyesi olmalı",
                adimlar: [
                    "Şantiye > Malzeme Geliş: Atölye/Fabrikadan giriş",
                    "Montaj ilerlemesi gir",
                    "Giderler > Birim: Şantiye/Proje > Proje Seç > Kalemler: İşçilik, Mazot, Elektrik, Sarf, SSK/Vergi, Bakım, Taşıma, Diğer - tarihli",
                    "Projeyi Kapat > Malzeme maliyeti + giderler vs satış fiyatı = Kar/Zarar"
                ],
                sonuc: "Proje bazlı kar/zarar raporu",
                sik_hata: "Giderleri toplu girmek, günlük tarihli girmemek",
                hedef_sayfa: "/santiye/montaj",
                sure: "3 dk",
                zorluk: "İleri",
                role: "manager",
                demo_data: "Proje: Marina Rezidans | Gelen Malzeme: 300 m² | İlerleme: %85 | Ek Gider: Montaj İşçilik 65.000 TL | Satış: 450.000 TL | Net Kar: +84.200 TL"
            },
            {
                id: "W10",
                kategori: "Maliyet Analizi",
                baslik: "Aylık Gerçek Maliyet",
                amac: "1 m2 ve 1 tonun gerçek maliyetini bilmek",
                on_kosul: "Ay boyunca gerçekleşen giderlerin ve üretimlerin girilmiş olması",
                adimlar: [
                    "Giderler sayfasına her gün giriş yapılır",
                    "Elektrik faturası geldiğinde: Ait Olduğu Ay'ı seç (Mayıs faturası Haziran'da gelse bile ait olduğu ay: Mayıs)",
                    "Maliyet Analizi > Ay Seç > Birim Seç",
                    "Sistem: O Ay Toplam Gider / O Ay Üretilen Ton veya m2 = Birim Maliyet",
                    "Stoktaki ürün üzerine maliyet biner"
                ],
                sonuc: "Ton ve m2 başına gerçek maliyet, satış fiyatı belirleme",
                sik_hata: "Elektrik faturasını geldiği aya yazmak, ait olduğu aya değil",
                hedef_sayfa: "/maliyet-analizi",
                sure: "2 dk",
                zorluk: "Stratejik",
                role: "manager",
                demo_data: "Dönem: Mayıs 2026 (Haziran faturası ile) | Fabrika Gideri: 1.420.000 TL | Çıkan: 5.680 m² | Birim Gerçek Maliyet: 250,00 TL/m²"
            }
        ],

        init() {
            this.loadState();
            this.$nextTick(() => {
                if (window.lucide) {
                    lucide.createIcons();
                }
            });
        },

        getCategoryColor(cat) {
            switch(cat) {
                case 'Ocak': return '#f59e0b';
                case 'Fabrika': return '#3b82f6';
                case 'Atölye': return '#10b981';
                case 'Şantiye': return '#8b5cf6';
                case 'Maliyet Analizi': return '#f43f5e';
                default: return '#2563eb';
            }
        },

        get filteredWorkflows() {
            return this.workflows.filter(wf => {
                const matchCategory = this.activeCategory === 'Tümü' || wf.kategori === this.activeCategory;
                const matchRole = this.selectedRole === 'all' || wf.role === this.selectedRole || (this.selectedRole === 'manager' && (wf.id === 'W1' || wf.id === 'W2' || wf.id === 'W8' || wf.id === 'W9' || wf.id === 'W10'));
                return matchCategory && matchRole;
            });
        },

        get completedCount() {
            return Object.values(this.doneWorkflows).filter(Boolean).length;
        },

        get progressPercent() {
            return Math.round((this.completedCount / this.workflows.length) * 100) || 0;
        },

        get checklistCompletedCount() {
            return this.checklist.filter(c => c.completed).length;
        },

        isWorkflowDone(id) {
            return !!this.doneWorkflows[id];
        },

        toggleWorkflowDone(id) {
            this.doneWorkflows[id] = !this.doneWorkflows[id];
            this.syncChecklistFromWorkflows();
            this.saveState();
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        toggleChecklistItem(item) {
            item.completed = !item.completed;
            if (item.targetWfId) {
                this.doneWorkflows[item.targetWfId] = item.completed;
                if (item.targetWfId === 'W4' && item.completed) {
                    this.doneWorkflows['W5'] = true;
                }
            }
            this.saveState();
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        syncChecklistFromWorkflows() {
            this.checklist[0].completed = !!this.doneWorkflows['W1'];
            this.checklist[1].completed = !!this.doneWorkflows['W2'];
            this.checklist[2].completed = !!(this.doneWorkflows['W4'] && this.doneWorkflows['W5']);
            this.checklist[3].completed = !!this.doneWorkflows['W7'];
            this.checklist[4].completed = !!this.doneWorkflows['W10'];
        },

        setCategory(cat) {
            this.activeCategory = cat;
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        setRole(r) {
            this.selectedRole = r;
            this.saveState();
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        toggleExpand(id) {
            this.expandedCard = this.expandedCard === id ? null : id;
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        scrollToWorkflow(wfId) {
            this.activeCategory = 'Tümü';
            this.expandedCard = wfId;
            this.$nextTick(() => {
                if (window.lucide) lucide.createIcons();
                const el = document.getElementById('card-' + wfId);
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            });
        },

        copyDemoData(text) {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(() => {
                    alert('Örnek veriler panoya kopyalandı:\n\n' + text);
                }).catch(() => {
                    prompt('Örnek veriyi kopyalayın:', text);
                });
            } else {
                prompt('Örnek veriyi kopyalayın:', text);
            }
        },

        voteWorkflow(id, dir) {
            this.votes[id] = dir;
            this.saveState();
            this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
        },

        getVote(id) {
            return this.votes[id];
        },

        resetProgress() {
            if (confirm('Öğrenme Merkezi ilerlemenizi ve tamamlanan akışları sıfırlamak istiyor musunuz?')) {
                this.doneWorkflows = {};
                this.checklist.forEach(c => c.completed = false);
                this.votes = {};
                this.saveState();
                this.$nextTick(() => { if (window.lucide) lucide.createIcons(); });
            }
        },

        saveState() {
            try {
                localStorage.setItem('ozerler_erp_learning_v1', JSON.stringify({
                    doneWorkflows: this.doneWorkflows,
                    checklist: this.checklist.map(c => ({ id: c.id, completed: c.completed })),
                    demoMode: this.demoMode,
                    selectedRole: this.selectedRole,
                    votes: this.votes
                }));
            } catch (e) {
                console.warn('localStorage save failed', e);
            }
        },

        loadState() {
            try {
                const raw = localStorage.getItem('ozerler_erp_learning_v1');
                if (raw) {
                    const data = JSON.parse(raw);
                    if (data.doneWorkflows) this.doneWorkflows = data.doneWorkflows;
                    if (data.demoMode !== undefined) this.demoMode = data.demoMode;
                    if (data.selectedRole) this.selectedRole = data.selectedRole;
                    if (data.votes) this.votes = data.votes;
                    if (Array.isArray(data.checklist)) {
                        data.checklist.forEach(savedItem => {
                            const found = this.checklist.find(c => c.id === savedItem.id);
                            if (found) found.completed = !!savedItem.completed;
                        });
                    }
                }
            } catch (e) {
                console.warn('localStorage load failed', e);
            }
        }
    };
}

// Global binding & Alpine registration
window.learningCenter = createLearningCenter;

if (window.Alpine) {
    window.Alpine.data('learningCenter', createLearningCenter);
} else {
    document.addEventListener('alpine:init', () => {
        Alpine.data('learningCenter', createLearningCenter);
    });
}
