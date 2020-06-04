using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

using AggieAir.Services;
using AggieAir.Models;
using Microsoft.EntityFrameworkCore.Metadata.Internal;

namespace AggieAir.Controllers
{
    public class DataController : Controller
    {
        private readonly DataService dataService;

        public DataController(DataService dataService)
        {
            this.dataService = dataService;
        }


        // GET: SensorData
        public ActionResult Index()
        {
            var items = dataService.Get().ToList();

            List<decimal> PM_ARRAY = new List<decimal>();
            List<decimal> LAT_ARRAY = new List<decimal>();
            List<decimal> LNG_ARRAY = new List<decimal>();
            //List<string> GPGLL_ARRAY = new List<string>();
            List<string> TIME_ARRAY = new List<string>();
            List<string> DATE_ARRAY = new List<string>();

            var count = 0;

            foreach (var each in items)
            {
                
                PM_ARRAY.Add(Convert.ToDecimal(each.PM25));

                string[] GPGLL_split = each.GPGLL.Split(new Char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);

                LAT_ARRAY.Add(decimal.Parse(GPGLL_split[0]));
                LNG_ARRAY.Add(decimal.Parse(GPGLL_split[1]));

                TIME_ARRAY.Add(each.TIME);
                DATE_ARRAY.Add(each.DATE);

                count++;

            }


            ViewBag.COUNT = count;

            ViewBag.PM_ARRAY = PM_ARRAY;
            ViewBag.LAT_ARRAY = LAT_ARRAY;
            ViewBag.LNG_ARRAY = LNG_ARRAY;
            ViewBag.TIME_ARRAY = TIME_ARRAY;
            ViewBag.DATE_ARRAY = DATE_ARRAY;

            return View(dataService.Get());
        }

        // GET: SensorData/Details/5
        public ActionResult Details(int id)
        {
            return View();
        }

        // GET: SensorData/Create
        public ActionResult Create()
        {
            return View();
        }

        // POST: SensorData/Create
        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult Create(SensorData sensordata)
        {


            if (ModelState.IsValid)
            {
                dataService.Create(sensordata);
                //return RedirectToAction(nameof(Index));
            }
            return View(sensordata);
        }


        // GET: SensorData/Edit/5
        public ActionResult Edit(int id)
        {
            return View();
        }

        // POST: SensorData/Edit/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Edit(int id, IFormCollection collection)
        {
            try
            {

                return RedirectToAction(nameof(Index));
            }
            catch
            {
                return View();
            }
        }

        // GET: SensorData/Delete/5
        public ActionResult Delete(int id)
        {
            return View();
        }

        // POST: SensorData/Delete/5
        [HttpPost]
        [ValidateAntiForgeryToken]
        public ActionResult Delete(int id, IFormCollection collection)
        {
            try
            {

                return RedirectToAction(nameof(Index));
            }
            catch
            {
                return View();
            }
        }
    }
}
